package me.wjz.nekocrypt.ui.screen

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.ui.ClickableSettingItem
import me.wjz.nekocrypt.ui.ColorSettingItem
import me.wjz.nekocrypt.ui.RangeSliderSettingItem
import me.wjz.nekocrypt.ui.SettingsHeader
import me.wjz.nekocrypt.ui.SliderSettingItem
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    var showAboutDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope() // 获取协程作用域，用于执行异步任务
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // verticalArrangement = Arrangement.spacedBy(16.dp), 选项之间不需要间隔
    ) {
        // 第一个分组：加解密设置
        item {
            SettingsHeader(stringResource(R.string.crypto_settings))
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SliderSettingItem(  //  长按发送密文所需时间
                key = SettingKeys.ENCRYPTION_LONG_PRESS_DELAY,
                defaultValue = 500L, // 默认 500 毫秒
                icon = { Icon(Icons.Outlined.Timer, contentDescription = "Long Press Delay") },
                title = stringResource(R.string.decryption_long_press_delay),
                subtitle = stringResource(R.string.decryption_long_press_delay_desc),
                valueRange = 50L..1000L, // 允许用户在 200ms 到 1500ms 之间选择
                step = 50L //每50ms一个挡位
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SliderSettingItem( //   点击密文解密的所需时间。
                key = SettingKeys.DECRYPTION_WINDOW_SHOW_TIME,
                defaultValue = 500L, // 默认 500 毫秒
                icon = { Icon(Icons.Outlined.Timer, contentDescription = "Long Press Delay") },
                title = stringResource(R.string.decryption_window_show_time),
                subtitle = stringResource(R.string.decryption_window_show_time_desc),
                valueRange = 500L..3000L,
                step = 250L // 单步步长
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            // 区间选择器
            RangeSliderSettingItem(
                minKey = SettingKeys.CIPHERTEXT_STYLE_LENGTH_MIN,
                maxKey = SettingKeys.CIPHERTEXT_STYLE_LENGTH_MAX,
                defaultMin = 3,
                defaultMax = 7,
                icon = { Icon(Icons.Outlined.GraphicEq, contentDescription = "Ciphertext Length") },
                title = stringResource(R.string.ciphertext_length_title),
                subtitle = stringResource(R.string.ciphertext_length_subtitle),
                valueRange = 1..10,
                step = 1
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        // ————————————————————————————————

        // 第二个分组，界面相关设置
        item {
            SettingsHeader(stringResource(R.string.crypto_ui_settings))
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SliderSettingItem( //   沉浸式下，密文位置更新间隔
                key = SettingKeys.DECRYPTION_WINDOW_POSITION_UPDATE_DELAY,
                defaultValue = 250L, // 默认 250
                icon = { Icon(Icons.Outlined.Timer, contentDescription = "position update delay") },
                title = stringResource(R.string.decryption_window_position_update_delay),
                subtitle = stringResource(R.string.decryption_window_position_update_delay_desc),
                valueRange = 0L..1000L,
                step = 50L // 单步步长
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        // ————————————————————————————————
        item {
            ColorSettingItem(
                key = SettingKeys.SEND_BTN_OVERLAY_COLOR,
                defaultValue = "#5066ccff",
                title = stringResource(R.string.send_btn_overlay_color),
                subtitle = stringResource(R.string.send_btn_overlay_color_desc)
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            SliderSettingItem(  //  控制拉起附件发送悬浮窗的时间间隔。
                key = SettingKeys.SHOW_ATTACHMENT_VIEW_DOUBLE_CLICK_THRESHOLD,
                defaultValue = 250L,
                icon = { Icon(Icons.Outlined.Timer, contentDescription = "Long Press Delay") },
                title = stringResource(R.string.double_click_threshold),
                subtitle = stringResource(R.string.double_click_threshold_desc),
                valueRange = 250L..1000L, // 允许用户在 200ms 到 1500ms 之间选择
                step = 250L //每50ms一个挡位
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }

        // 第二个分组：关于
        item {
            SettingsHeader("关于")
        }
        item {
            ClickableSettingItem(
                icon = { Icon(Icons.Default.Info, contentDescription = "About App") },
                title = "关于 NekoCrypt",
                onClick = { showAboutDialog=true }
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            val versionName = context.packageManager.getPackageInfo(context.packageName, 0).versionName
            ClickableSettingItem(
                icon = { Icon(Icons.Default.Build, contentDescription = "Version") },
                // ✨ 在 title 的 Composable 槽位里，自定义我们的布局！
                title = stringResource(R.string.version,versionName?:"unknown"),
                onClick = { handleCheckUpdate(context, scope, versionName?:"N/A") }
            )
        }
        item {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )
        }
        item {
            ClickableSettingItem(
                icon = { Icon(Icons.Default.Link, contentDescription = "GitHub Link") },
                title = stringResource(R.string.github),
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, "https://github.com/WJZ-P/NekoCrypt".toUri())
                    context.startActivity(intent)
                }
            )
        }

    }

    if(showAboutDialog){
        AboutDialog(onDismissRequest = {showAboutDialog = false})
    }
}

/**
 * 用于显示关于信息的对话框
 */
@Composable
private fun AboutDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Default.Info, contentDescription = null) },
        title = { Text(text = stringResource(R.string.about_dialog_title)) },
        text = {
            Column {
                Text(stringResource(R.string.about_dialog_content))
                // 你可以在这里添加更多信息，比如版本号、作者、开源链接等
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.accept))
            }
        }
    )
}

/**
 * 一个用于解析 GitHub API /releases/latest 端点返回的 JSON 的数据类。
 * @Serializable 注解让它可以被 kotlinx.serialization 库处理。
 * @SerialName 注解用于将 JSON 中的 snake_case 字段名映射到我们的 camelCase 属性名。
 */
@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String, // 版本标签，例如 "v1.1.0"

    @SerialName("html_url")
    val htmlUrl: String, // 该发布页面的网址
)

private fun handleCheckUpdate(context: Context, scope: CoroutineScope, versionName: String) {
    scope.launch {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, context.getString(R.string.checking_for_update), Toast.LENGTH_SHORT).show()
        }

        // 切换到 IO 线程执行网络请求
        val latestRelease: GitHubRelease? = withContext(Dispatchers.IO) {
            try {
                val url = URL("https://api.github.com/repos/WJZ-P/NekoCrypt/releases/latest")
                val connection = url.openConnection() as HttpURLConnection
                val jsonText = connection.inputStream.bufferedReader().use(BufferedReader::readText)
                Log.d(NekoCryptApp.TAG,jsonText)

                // 使用 kotlinx.serialization 解析 JSON
                Json { ignoreUnknownKeys = true }.decodeFromString<GitHubRelease>(jsonText)

            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }

        // 回到主线程更新 UI
        withContext(Dispatchers.Main) {
            if (latestRelease == null) {
                Toast.makeText(context, context.getString(R.string.check_for_update_failed), Toast.LENGTH_SHORT).show()
                return@withContext
            }

            // 比较版本号（简单地移除 'v' 前缀进行比较）
            val latestVersionName = latestRelease.tagName.removePrefix("v")

            if (versionName != "N/A" && latestVersionName > versionName) {
                Toast.makeText(context, context.getString(R.string.check_for_update_failed,latestRelease.tagName), Toast.LENGTH_SHORT).show()
                // 引导用户去发布页面查看
                val intent = Intent(Intent.ACTION_VIEW, latestRelease.htmlUrl.toUri())
                context.startActivity(intent)
            } else {
                Toast.makeText(context, context.getString(R.string.is_newest_version), Toast.LENGTH_SHORT).show()
            }
        }
    }
}