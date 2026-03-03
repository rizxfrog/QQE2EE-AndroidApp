package me.wjz.nekocrypt.service.handler

import android.content.ContentValues
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.dianming.phoneapp.MyAccessibilityService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.dialog.FilePreviewDialog
import me.wjz.nekocrypt.util.CryptoDownloader
import me.wjz.nekocrypt.util.NCFileProtocol
import me.wjz.nekocrypt.util.NCWindowManager
import me.wjz.nekocrypt.util.getCacheFileFor
import me.wjz.nekocrypt.util.getUriForFile
import java.io.IOException

/**
 * 点击文件or图片按钮后的处理类，负责控制悬浮窗的生命周期，并负责下载，展示等逻辑
 */
class FileActionHandler(private val service: MyAccessibilityService) {
    private val tag ="NCFileActionHandler"
    private var dialogManager: NCWindowManager? = null
    private var downloadProgress by mutableStateOf<Int?>(null)
    private var downloadedFileUri by mutableStateOf<Uri?>(null)
    private var isImageSavedThisTime by mutableStateOf(false)

    /**
     * 显示文件预览对话框
     */
    fun show(fileInfo: NCFileProtocol) {
        dismiss() // 先关闭旧的

        // 根据文件信息生成本地缓存的唯一路径
        val targetFile = getCacheFileFor(service,fileInfo)

        // 检查缓存文件是否完整
        if (targetFile.exists() && targetFile.length() == fileInfo.size) {
            Log.d(tag, "文件已在缓存中找到: ${targetFile.path}")
            // ✨ 如果缓存命中，直接为文件生成安全的Uri
            downloadedFileUri = getUriForFile(service,targetFile)
            downloadProgress = null
        } else {
            Log.d(tag, "文件未缓存或不完整，准备下载。")
            downloadedFileUri = null // 未缓存，重置状态
            downloadProgress = null
        }
        // 创建视图
        dialogManager = NCWindowManager(
            context = service,
            onDismissRequest = { dialogManager = null },
            anchorRect = null
        ) {
            FilePreviewDialog(
                fileInfo = fileInfo,
                downloadProgress = downloadProgress, // ✨ 将进度状态传递给UI
                downloadedFileUri = downloadedFileUri, // nullable
                isImageSavedThisTime = isImageSavedThisTime, // 本次会话中是否把图片保存到了系统相册
                onDismissRequest = { dismiss() },
                onDownloadRequest = { info ->
                    startDownload(info)
                },
                onOpenRequest = { uri ->
                    openFile(uri,fileInfo) // ✨ 回调现在直接使用 Uri
                },
                onSaveToGalleryRequest = {uri ->
                    service.serviceScope.launch {
                        isImageSavedThisTime = saveImageToGallery(uri, fileInfo)
                    }
                }
            )
        }
        dialogManager?.show()
    }

    /**
     * 关闭对话框
     */
    fun dismiss() {
        dialogManager?.dismiss()
        dialogManager = null
    }

    /**
     * 启动文件下载
     */
    private fun startDownload(fileInfo: NCFileProtocol) {
        if(downloadProgress != null)  return // 保证健壮性，防止重复点击

        service.serviceScope.launch {
            val targetFile = getCacheFileFor(service,fileInfo)
            try{
                downloadProgress = 0
                // download会suspend。
                val result = CryptoDownloader.download(
                    fileInfo = fileInfo,
                    targetFile = targetFile,
                    onProgress = { progress -> downloadProgress = progress }
                )

                if(result.isSuccess){
                    val file = result.getOrThrow()
                    // ✨ 下载成功后，为新文件生成安全的Uri并更新状态
                    downloadedFileUri = getUriForFile(service,file)
                    Log.d(tag, "文件下载成功，Uri: $downloadedFileUri")
                }else{
                    val error = result.exceptionOrNull()?.message ?: "未知错误"
                    Log.e(tag, "文件下载失败: $error")
                    showToast(service.getString(R.string.dialog_download_file_download_failed, error))
                }
            } finally {
                downloadProgress = null
            }
        }
    }

    suspend fun showToast(string: String, duration: Int = Toast.LENGTH_SHORT) {
        Log.d(tag, "showToast: $string")
        withContext(Dispatchers.Main) {
            Toast.makeText(service.applicationContext, string, duration).show()
        }
    }

    private fun openFile(uri: Uri,fileInfo: NCFileProtocol){
        service.serviceScope.launch {
            try{
                // 1. ✨ 从原始文件名中获取文件后缀
                val extension = fileInfo.name.substringAfterLast('.', "")
                // 2. ✨ 使用 MimeTypeMap 将后缀转换为标准的MIME类型
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                    ?: "*/*" // 如果找不到，使用通用类型

                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri,mimeType)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                service.startActivity(intent)
                dismiss() // 选择打开文件的话，就要关闭当前的悬浮窗

            } catch (e: Exception) {
                Log.e(tag, "打开文件失败", e)
                showToast(service.getString(R.string.cannot_open_file))
            }
        }
    }

    // 根据uri和文件名保存到系统相册，并返回操作结果。
    private suspend fun saveImageToGallery(uri: Uri, fileInfo: NCFileProtocol): Boolean {

        val success = withContext(Dispatchers.IO) {
            runCatching {
                val extension = fileInfo.name.substringAfterLast('.', "")
                val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

                // ContentValues 就像一个“档案袋”，我们把新文件的所有信息（元数据）都放进去。
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileInfo.name)      // 文件在相册里显示的名字。
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)         // 文件的mime类型
                    // 档案3 & 4 (仅限 Android 10 及以上)：
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        // 告诉系统要把这个文件放在公共的“相册”文件夹里。
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                        // 先把文件标记为“待定”状态。这意味着在文件内容被完全写入之前，
                        // 其他应用（包括相册自己）是看不到这个文件的，可以防止出现损坏的半成品文件。
                        put(MediaStore.MediaColumns.IS_PENDING, 1)
                    }
                }

                // 用我们写好的信息，去申请一个URI
                val imageUri = service.contentResolver.insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    contentValues
                )
                    ?: throw IOException("无法在相册中创建新文件。")

                // 使用我们新的imageUri，写入文件
                service.contentResolver.openOutputStream(imageUri).use { outputStream ->
                    service.contentResolver.openInputStream(uri).use { inputStream ->
                        requireNotNull(inputStream) { "无法打开缓存文件的输入流" }
                        requireNotNull(outputStream) { "无法打开相册文件的输出流" }
                        inputStream.copyTo(outputStream)
                    }
                }

                // (仅限 Android 10 及以上) 文件内容已经写完，我们再次更新档案，
                // 把“待定”状态改为0，正式通知系统：“文件已准备就绪，可以对外展示了！”
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    service.contentResolver.update(imageUri, contentValues, null, null)
                }

                //顺利完成，返回true
                true
            }.onFailure { e ->
                Log.e(tag, "保存图片到相册失败", e)
                false // 返回失败
            }.getOrDefault(false) // 拿不到，默认就返回false
        }
        if (success) showToast(service.getString(R.string.image_saved_to_gallery_success))
        else showToast(service.getString(R.string.image_saved_to_gallery_failed))
        return success
    }
}

