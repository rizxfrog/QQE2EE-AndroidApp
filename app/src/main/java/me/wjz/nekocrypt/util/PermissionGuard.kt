package me.wjz.nekocrypt.util

import PermissionDialog
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.SyncProblem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.dialog.NCDialog

@Composable
fun PermissionGuard(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var activeDialog by remember { mutableStateOf<NCDialog?>(null) }

    //  用于跳转到应用详细信息，方便设置权限
    val appDetailsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {}
    //  跳转到“显示在其他应用上层”权限设置页面
    val overlayPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (!PermissionUtil.isOverlayPermissionGranted(context)) {
            // 弹出对话引导用户跳转到权限设置页面
            activeDialog = PermissionDialog(
                dialogIcon = Icons.Outlined.SyncProblem,
                dialogTitle = context.getString(R.string.permission_still_missing_title),
                dialogText = context.getString(R.string.permission_still_missing_text),
                onDismissRequest = { activeDialog = null },
                onConfirmRequest = {
                    appDetailsLauncher.launch(
                        Intent(
                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                            "package:${context.packageName}".toUri()
                        )
                    )
                    activeDialog = null
                }
            )
        }
    }

    // UI首次加载的时候做一次权限检查
    LaunchedEffect(Unit) {
        if (!PermissionUtil.isOverlayPermissionGranted(context)) {
            // 弹出对话引导用户跳转到权限设置页面
            activeDialog = PermissionDialog(
                dialogIcon = Icons.Outlined.Layers,
                dialogTitle = context.getString(R.string.permission_overlay_title),
                dialogText = context.getString(R.string.permission_overlay_text),
                onDismissRequest = { activeDialog = null },
                onConfirmRequest = {
                    overlayPermissionLauncher.launch(
                        Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                    )
                    activeDialog = null
                }

            )
        }
    }

    content()// 渲染传入的页面

    activeDialog?.Content()// 根据权限状态拉起对话框。
}