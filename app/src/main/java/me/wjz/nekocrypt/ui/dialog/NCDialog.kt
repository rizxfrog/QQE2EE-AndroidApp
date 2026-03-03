package me.wjz.nekocrypt.ui.dialog

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 弹窗对话栏
 */
interface NCDialog {
    val icon: ImageVector
    val title: String
    val text: String
    val onDismiss: () -> Unit
    val onConfirm: () -> Unit
    @Composable
    fun Content()
}