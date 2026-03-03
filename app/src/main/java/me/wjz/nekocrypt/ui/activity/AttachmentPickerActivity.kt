package me.wjz.nekocrypt.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.util.ResultRelay
import java.io.File
import java.io.IOException

class AttachmentPickerActivity : ComponentActivity() {
    private val tag = "AttachmentPickerActivity"

    companion object {
        const val EXTRA_PICK_TYPE = "pick_type"
        const val TYPE_MEDIA = "media"   // 图+视频
        const val TYPE_FILE = "file"    // 任意文件
    }

    private lateinit var mediaPicker: ActivityResultLauncher<PickVisualMediaRequest>
    private lateinit var filePicker: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // 必须不能抢占焦点，否则handler检测到不是目标应用界面就会杀掉自己
        window.addFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)

        /**
         * 这里的逻辑演进说明：
         * 1. 最初方案是直接返回用户选择的Uri。这在文件较小时可行，因为当时的做法是立刻将文件完整读入内存。
         * 2. 为了支持大文件并避免内存溢出，我们改用了流式上传。但流式读取过程较慢。
         * 3. 这就暴露了安卓的临时Uri权限问题：当Activity在返回Uri后立刻finish()，它获得的临时访问权限很快就会失效。
         * 导致后台的Service在稍后进行流式读取时，会因为权限丢失而失败 (SecurityException)。
         * 4. 因此，最终方案是：在本Activity中，趁着临时权限还生效，立刻将文件复制一份到我们App自己的私有缓存目录。
         * 然后返回这个缓存文件的、我们拥有永久访问权的Uri。这样后台服务就可以随时、安全地进行流式读取了。
         */
        val onResult = { uri: Uri? ->
            if (uri != null) {
                // 当拿到结果时，在IO线程中复制一份文件，然后发回我们复制出来的文件的uri。
//                lifecycleScope.launch(Dispatchers.IO) {
//                    try {
//                        val newCacheUri = copyFileToCache(uri)
//                        ResultRelay.send(newCacheUri)
//                        Log.d(tag, "已发送缓存文件的Uri：$newCacheUri")
//                    } catch (e: Exception) {
//                        Log.e(tag, "复制文件到缓存失败", e)
//                        // 确保UI操作在主线程
//                        withContext(Dispatchers.Main) {
//                            Toast.makeText(
//                                this@AttachmentPickerActivity,
//                                getString(R.string.crypto_attachment_file_not_accessible),
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//                    } finally {
//                        // 无论成功或失败，最后都关闭Activity
//                        withContext(Dispatchers.Main) {
//                            finish()
//                        }
//                    }
//                }

                lifecycleScope.launch {
                    try {
                        // 在拿到Uri后，立刻申请持久化读取权限
                        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION
                        contentResolver.takePersistableUriPermission(uri, takeFlags)
                        Log.d(tag, "已成功获取持久化权限: $uri")
                        // 将这个现在拥有持久权限的Uri发送出去
                        ResultRelay.send(uri)

                    } catch (e: SecurityException) {
                        Log.e(tag, "申请持久化权限失败，硬发Uri", e)
                        ResultRelay.send(uri)
                    } finally {
                        // ✨ 关键修复：在关闭Activity前增加一个微小的延迟
                        // 这给了系统足够的时间来处理持久化权限的授予，
                        // 防止在Service尝试访问Uri之前，权限就因Activity销毁而失效。
                        delay(200)
                        finish()
                    }
                }
                Unit
            }
            else{
                Log.d(tag, "用户取消了文件选择，关闭Activity。")
                finish()
            }
        }

        // 注册文件选择器，并绑定我们统一的 `onResult` 处理逻辑
        mediaPicker = registerForActivityResult(
            ActivityResultContracts.PickVisualMedia(),
            onResult
        )
        filePicker = registerForActivityResult(
            ActivityResultContracts.GetContent(),
            onResult
        )

        // 根据启动意图，调用对应的文件选择器
        when (intent.getStringExtra(EXTRA_PICK_TYPE)) {
            TYPE_MEDIA -> mediaPicker.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
            )

            TYPE_FILE -> filePicker.launch("*/*")
            else -> {
                // 如果没有指定类型，默认关闭
                Log.w(tag, "未指定有效的PICK_TYPE，Activity将关闭。")
                finish()
            }
        }
    }

    /**
     * 将给定的Uri指向的文件复制到应用的内部缓存目录。
     * @param sourceUri 用户选择的文件的临时Uri。
     * @return 指向缓存目录中新文件的、我们拥有永久权限的Uri。
     * @throws IOException 如果文件读写失败。
     */
    @Throws(IOException::class)
    private fun copyFileToCache(sourceUri: Uri): Uri {
        // 通过ContentResolver打开源文件的输入流
        val inputStream = contentResolver.openInputStream(sourceUri)
            ?: throw IOException("无法为所选文件打开输入流。")

        // 在我们的缓存目录里创建一个唯一的文件名
        val fileName = "upload_cache_${System.currentTimeMillis()}"
        val tempFile = File(cacheDir, fileName)

        // 使用Kotlin的扩展函数，安全地将输入流复制到输出流，并自动关闭它们
        inputStream.use { input ->
            tempFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        // 返回我们新创建的、拥有完全权限的文件的Uri
        return Uri.fromFile(tempFile)
    }
}
