package me.wjz.nekocrypt.ui.screen

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import com.dianming.phoneapp.MyAccessibilityService
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.AppRegistry
import me.wjz.nekocrypt.Constant.DEFAULT_SECRET_KEY
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys.CURRENT_KEY
import me.wjz.nekocrypt.SettingKeys.SCAN_BTN_ACTIVE
import me.wjz.nekocrypt.data.LocalDataStoreManager
import me.wjz.nekocrypt.data.rememberCustomAppListState
import me.wjz.nekocrypt.hook.rememberDataStoreState
import me.wjz.nekocrypt.service.handler.ChatAppHandler
import me.wjz.nekocrypt.ui.SettingsHeader
import me.wjz.nekocrypt.ui.SwitchSettingItem
import me.wjz.nekocrypt.ui.dialog.AppHandlerInfoDialog
import me.wjz.nekocrypt.ui.dialog.KeyManagementDialog
import me.wjz.nekocrypt.util.PermissionUtil.isAccessibilityServiceEnabled
import me.wjz.nekocrypt.util.openAccessibilitySettings

@Composable
fun KeyScreen(modifier: Modifier = Modifier) {
    // 状态管理
    var currentKey by rememberDataStoreState(CURRENT_KEY, DEFAULT_SECRET_KEY)
    var showKeyDialog by remember { mutableStateOf(false) }     //控制密钥管理对话框的显示和隐藏
    // 自定义APP列表
    val customApps by rememberCustomAppListState()
    val context = LocalContext.current
    val dataStoreManager = LocalDataStoreManager.current
    val scope = rememberCoroutineScope() // 获取协程作用域，用于执行删除操作
    //  进入UI时做一些判断逻辑
    LaunchedEffect(Unit) {
        if (!isAccessibilityServiceEnabled(context) || !Settings.canDrawOverlays(context)) {
            dataStoreManager.saveSetting(SCAN_BTN_ACTIVE, false)
            Log.d(NekoCryptApp.TAG, "权限不足，已强制关闭扫描开关。")
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. 密钥选择器
        item {
            KeySelector(
                selectedKeyName = currentKey,
                onClick = {
                    // 当点击时，将状态设置为true，以显示对话框
                    showKeyDialog = true
                }
            )
        }

        // 支持的应用
        item {
            SettingsHeader(stringResource(R.string.key_screen_supported_app))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }

        item{
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                // 在 Card 内部使用 Column 来垂直排列我们的 App 列表项
                Column(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp), // 给上下一点内边距
                    verticalArrangement = Arrangement.spacedBy(12.dp) // 垂直项间距
                ) {
                    AppRegistry.allHandlers.forEach { handler ->
                        SupportedAppItem(handler = handler)
                    }
                }
            }
        }
        // 说明文本
        item {
          Row(
              modifier=Modifier.fillMaxWidth()
                  .padding(horizontal = 8.dp)
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                  .padding(horizontal = 12.dp, vertical = 8.dp), // 设置内边距
              verticalAlignment = Alignment.CenterVertically
          ){
              // 左侧的小图标
              Icon(
                  imageVector = Icons.Default.Info,
                  contentDescription = null, // 图标是纯装饰性的
                  tint = MaterialTheme.colorScheme.onTertiaryContainer,
                  modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              // 右侧的说明文字
              Text(
                  text = stringResource(R.string.key_screen_supported_app_description),
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onTertiaryContainer
              )
          }
        }
        // 自定义应用
        item {
            SettingsHeader(stringResource(R.string.key_screen_custom_app))
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        //  自定义APP列表
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = 16.dp, horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 根据列表是否为空，显示不同的内容
                    if (customApps.isEmpty()) {
                        Text(
                            text = stringResource(R.string.key_screen_no_custom_app_configured),
                            // 为了让单行文本在卡片内居中，我们给它一个 Modifier
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp) // 给一点垂直padding，避免太贴近按钮
                                .align(Alignment.CenterHorizontally),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // 遍历自定义应用列表，显示每一项
                        customApps.forEach { customHandler ->
                            SupportedAppItem(handler = customHandler,
                                onDelete = {
                                    scope.launch {
                                        dataStoreManager.deleteCustomApp(customHandler.packageName)
                                        Toast.makeText(context, context.getString(R.string.key_screen_delete_config_toast), Toast.LENGTH_SHORT).show()
                                    }
                                })
                        }
                    }
                }
            }
        }

        item{
            SwitchSettingItem(
                key = SCAN_BTN_ACTIVE,
                defaultValue = false,
                title = stringResource(R.string.enable_scanner_mode),
                subtitle = stringResource(R.string.enable_scanner_mode_description),
                icon = { Icon(imageVector = Icons.Default.MyLocation, contentDescription = stringResource(R.string.enable_scanner_mode)) },
                // ✨ 1. 这里是我们的“门卫”，负责所有权限检查
                onCheckValidated = { desiredState ->
                    // 如果是想关闭开关，永远允许
                    if (!desiredState) return@SwitchSettingItem true

                    // --- 下面都是想打开开关时的检查 ---
                    // 检查无障碍权限
                    if (!isAccessibilityServiceEnabled(context)) {
                        Toast.makeText(context, context.getString(R.string.please_grant_accessibility_service_permission), Toast.LENGTH_LONG).show()
                        // 最好再加一个跳转，方便用户开启
                        openAccessibilitySettings(context)
                        return@SwitchSettingItem false // 验证不通过，拦截！
                    }

                    // 检查悬浮窗权限
                    if (!Settings.canDrawOverlays(context)) {
                        Toast.makeText(context, context.getString(R.string.please_grant_overlay_permission), Toast.LENGTH_LONG).show()
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            "package:${context.packageName}".toUri()
                        )
                        context.startActivity(intent)
                        return@SwitchSettingItem false // 验证不通过，拦截！
                    }

                    // 所有检查都通过了，放行！
                    return@SwitchSettingItem true
                },

                // ✨ 2. 这里是我们的“执行官”，只在状态成功改变后执行
                onStateChanged = { isEnabled ->
                    // 根据最终的状态，发送不同的指令给服务
                    val action = if (isEnabled) {
                        MyAccessibilityService.ACTION_SHOW_SCANNER
                    } else {
                        MyAccessibilityService.ACTION_HIDE_SCANNER
                    }
                    val intent = Intent(context, MyAccessibilityService::class.java).apply {
                        this.action = action
                    }
                    context.startService(intent)
                }
            )
        }
    }

    if(showKeyDialog){
        KeyManagementDialog(onDismissRequest = { showKeyDialog = false })
    }
}

@Composable
fun SupportedAppItem(handler: ChatAppHandler, onDelete: (() -> Unit)? = null){
    var isEnabled by rememberDataStoreState(booleanPreferencesKey("app_enabled_${handler.packageName}"),
        defaultValue = true
    )
    var showHandlerInfoDialog by remember { mutableStateOf(false) }    //控制是否展示handler详细信息

    val context = LocalContext.current
    var appIcon by remember { mutableStateOf<Drawable?>(null) }
    var isAppInstalled by remember { mutableStateOf(false) }
    var appName by remember { mutableStateOf("") }
    // 尝试获取应用图标和名称
    LaunchedEffect(handler.packageName) {
        try{
            val pm = context.packageManager
            // 1. 先获取应用的“身份证” (ApplicationInfo)
            val appInfo = pm.getApplicationInfo(handler.packageName, 0)
            // 2. 从“身份证”里同时获取“照片”和“姓名”
            appIcon = pm.getApplicationIcon(appInfo)
            appName = pm.getApplicationLabel(appInfo).toString()

            isAppInstalled = true
        }catch (e: PackageManager.NameNotFoundException) {
            appIcon = null
            isAppInstalled = false
            Log.e(NekoCryptApp.TAG, e.toString())
        }
    }

    if (showHandlerInfoDialog) {
        AppHandlerInfoDialog(
            appName=appName,
            handler = handler,
            onDismissRequest = { showHandlerInfoDialog = false },
            onDeleteRequest = onDelete
        )
    }

    Card(
        onClick = { showHandlerInfoDialog = true },
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if(isAppInstalled&& appIcon!=null){
                Image(
                    // 用Google的Accompanist
                    painter = rememberDrawablePainter(drawable = appIcon),
                    contentDescription = "$appName 图标", // ✨ 使用 handler.name
                    modifier = Modifier.size(48.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "app not install", modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // 然后放APP名和包名
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    appName + if (!isAppInstalled) " — ${stringResource(R.string.not_installed)}" else "",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    handler.packageName,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(0.5f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            // 右边放开关
            Switch(
                checked = isEnabled,
                onCheckedChange = {
                    isEnabled = it
                    //Log.d(NekoCryptApp.TAG, "包${handler.packageName}监听状态：$it")
                },
                // ✨ 如果App没安装，开关就禁用
                enabled = isAppInstalled
            )
        }
    }
}
