
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.dialog.NCDialog

/**
 * 专门用于请求权限的对话框实现。
 * 它在构造时接收所有必要信息，并直接在 Content() 方法中定义自己的UI。
 */
class PermissionDialog(
    private val dialogIcon: ImageVector,
    private val dialogTitle: String,
    private val dialogText: String,
    private val onDismissRequest: () -> Unit,
    private val onConfirmRequest: () -> Unit
) : NCDialog {

    // 将接口属性映射到构造函数参数
    override val icon: ImageVector get() = dialogIcon
    override val title: String get() = dialogTitle
    override val text: String get() = dialogText
    override val onDismiss: () -> Unit get() = onDismissRequest
    override val onConfirm: () -> Unit get() = onConfirmRequest

    @Composable
    override fun Content() {
        AlertDialog(
            onDismissRequest = onDismiss,
            icon = { Icon(imageVector = icon, contentDescription = title) },
            title = { Text(text = title) },
            text = { Text(text = text) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(stringResource(R.string.permission_go_to_settings))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}