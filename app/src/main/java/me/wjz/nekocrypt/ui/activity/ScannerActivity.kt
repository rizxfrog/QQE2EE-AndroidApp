package me.wjz.nekocrypt.ui.activity

import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import me.wjz.nekocrypt.Constant.SCAN_RESULT
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.service.handler.CustomAppHandler
import me.wjz.nekocrypt.ui.dialog.ScannerDialog
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme


/**
 * 一个用于封装单个被找到的节点信息的数据类。
 * @param className 节点的类名 (e.g., "android.widget.EditText")。
 * @param resourceId 节点的资源 ID (e.g., "com.tencent.mm:id/input_editor")，可能为空。
 * @param text 节点的文本内容，可能为空。
 * @param contentDescription 节点的内容描述（常用于无障碍），可能为空。
 */
@Parcelize
data class FoundNodeInfo(
    val className: String,
    val resourceId: String?,
    val text: String?,
    val contentDescription: String?,
) : Parcelable

/**
 * ✨ 全新：用于封装单个消息列表及其内部消息文本的数据类。
 * 这就是我们的“房子和居民”情报。
 * @param listContainerInfo 消息列表容器节点本身的信息。
 * @param messageTexts 在这个容器内部找到的所有消息文本节点列表。
 */
@Parcelize
data class MessageListScanResult(
    val listContainerInfo: FoundNodeInfo,
    val messageTexts: List<FoundNodeInfo>
) : Parcelable

/**
 * ✨ 升级版：用于封装扫描结果的数据类。
 * @param packageName 当前应用的包名。
 * @param name 当前应用的可读名称 (e.g., "xx聊天")。
 * @param foundInputNodes 扫描到的所有可能的输入框节点列表。
 * @param foundSendBtnNodes 扫描到的所有可能的发送按钮节点列表。
 * @param foundMessageLists 扫描到的所有消息列表及其内部消息的集合。
 */
@Parcelize
data class ScanResult(
    val packageName: String,
    val name: String,
    val foundInputNodes: List<FoundNodeInfo>,
    val foundSendBtnNodes: List<FoundNodeInfo>,
    val foundMessageLists: List<MessageListScanResult>, // ✨ 结构变更
) : Parcelable


class ScannerDialogActivity: ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dataStoreManager = (application as NekoCryptApp).dataStoreManager

        // ✨ 核心魔法：从送来的“快递盒”(Intent)中，把名叫"scan_result"的“包裹”取出来
        val scanResult = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // 对于 Android 13 (API 33) 及以上版本，使用新的、类型安全的方法
            // 我们需要明确告诉系统，我们想取出来的是一个 ScanResult 类型的包裹
            intent.getParcelableExtra(SCAN_RESULT, ScanResult::class.java)
        } else {
            // 对于旧版本，使用传统的方法
            @Suppress("DEPRECATION") // 告诉编译器，我们知道这个方法过时了，但为了兼容性还是要用
            intent.getParcelableExtra<ScanResult>(SCAN_RESULT)  //  这里保留一下类型指定？看日志似乎是类型不确定导致的崩溃
        }

        if(scanResult == null){
            //
            Toast.makeText(this, getString(R.string.scanner_get_result_fail), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setContent {
            NekoCryptTheme {

                // 在这里显示我们的对话框
                // 当对话框请求关闭时，我们直接结束这个透明的 Activity
                ScannerDialog(scanResult,onDismissRequest = { finish() }, onConfirm ={ scanSelections,scanResult ->
                    lifecycleScope.launch {
                        val newHandler = CustomAppHandler(
                            packageName = scanResult.packageName,
                            inputId = scanSelections.inputNode.resourceId ?: "",
                            sendBtnId = scanSelections.sendBtnNode.resourceId ?: "",
                            messageTextId = scanSelections.messageText.resourceId ?: "",
                            messageListClassName = scanSelections.messageList.className
                        )

                        dataStoreManager.addCustomApp(newHandler)
                        // 3. 给出成功提示并关闭窗口
                        Toast.makeText(
                            this@ScannerDialogActivity,
                            getString(R.string.scanner_config_saved_toast),
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                })
            }
        }
    }
}