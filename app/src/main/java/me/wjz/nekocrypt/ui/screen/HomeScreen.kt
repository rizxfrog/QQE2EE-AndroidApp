package me.wjz.nekocrypt.ui.screen

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import me.wjz.nekocrypt.CommonKeys.DECRYPTION_MODE_IMMERSIVE
import me.wjz.nekocrypt.CommonKeys.DECRYPTION_MODE_STANDARD
import me.wjz.nekocrypt.CommonKeys.ENCRYPTION_MODE_IMMERSIVE
import me.wjz.nekocrypt.CommonKeys.ENCRYPTION_MODE_STANDARD
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.hook.rememberDataStoreState
import com.dianming.phoneapp.MyAccessibilityService
import me.wjz.nekocrypt.ui.InfoDialogIcon
import me.wjz.nekocrypt.ui.RadioOption
import me.wjz.nekocrypt.ui.SegmentedButtonSetting
import me.wjz.nekocrypt.ui.SwitchSettingCard
import me.wjz.nekocrypt.ui.component.CatPawButton
import me.wjz.nekocrypt.util.openAccessibilitySettings
import me.wjz.nekocrypt.util.rememberAccessibilityServiceState

// --- 主屏幕代码 ---

@Composable
fun HomeScreen(modifier: Modifier = Modifier) {
    // 1. 获取当前上下文
    val context: Context = LocalContext.current

    // 2. 使用我们新的 Composable 函数来获取并监听无障碍服务的状态
    val isAccessibilityEnabled by rememberAccessibilityServiceState(
        context,
        MyAccessibilityService::class.java
    )

    val useAutoEncryption by rememberDataStoreState(SettingKeys.USE_AUTO_ENCRYPTION, false)
    val useAutoDecryption by rememberDataStoreState(SettingKeys.USE_AUTO_DECRYPTION, false)

    // 使用 Column 作为根布局，以垂直排列组件
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 使用带权重的 Column 来包裹原有的猫爪UI，使其占据大部分空间并保持居中
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ✨ 核心修正：用一个 Box 把猫爪按钮包裹起来，并给这个 Box 一个“方形模具”
            Box(
                modifier = Modifier
                    .fillMaxWidth() // 让 Box 尽可能宽
                    .padding(52.dp) // 给猫爪留出一些呼吸空间
                    .aspectRatio(1f), // ✨ 魔法！强制这个 Box 的高度等于它的宽度，永远保持正方形
                contentAlignment = Alignment.Center
            ) {
                CatPawButton(
                    modifier = Modifier.fillMaxSize(), // 让猫爪按钮填满这个完美的正方形
                    isEnabled = isAccessibilityEnabled,
                    statusText = if (isAccessibilityEnabled)
                        stringResource(id = R.string.accessibility_service_enabled)
                    else
                        stringResource(id = R.string.accessibility_service_disabled),
                    onClick = { openAccessibilitySettings(context) }
                )
            }
        }

        // 在底部添加我们的设置卡片

        // 加密选项
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                SwitchSettingCard(
                    key = SettingKeys.USE_AUTO_ENCRYPTION,
                    defaultValue = false,
                    title = stringResource(id = R.string.setting_encrypt_on_send_title),
                    subtitle = stringResource(id = R.string.setting_encrypt_on_send_subtitle)
                )

                // 2. 模式选择
                AnimatedVisibility(
                    visible = useAutoEncryption,
                    enter = expandVertically(animationSpec = tween(400)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut()
                ) {
                    val modeStandardText = stringResource(R.string.mode_standard)
                    val modeImmersiveText = stringResource(R.string.mode_immersive)

                    val encryptionModeOptions = remember {
                        listOf(
                            RadioOption(ENCRYPTION_MODE_STANDARD, modeStandardText),
                            RadioOption(ENCRYPTION_MODE_IMMERSIVE, modeImmersiveText)
                        )
                    }
                    SegmentedButtonSetting(
                        settingKey = SettingKeys.ENCRYPTION_MODE,
                        defaultOptionKey = ENCRYPTION_MODE_STANDARD,
                        title = stringResource(id = R.string.setting_encryption_mode_info_title),
                        options = encryptionModeOptions,
                        titleExtraContent = {   // 标题旁边的额外内容。
                            InfoDialogIcon(
                                title = stringResource(R.string.setting_encryption_mode_info_text),
                                text = stringResource(R.string.setting_encryption_mode_info_desc),
                                contentDescription = stringResource(R.string.setting_encryption_mode_info_desc)
                            )
                        }
                    )
                }
            }
        }

        // 解密选项
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                //解密开关
                SwitchSettingCard(
                    key = SettingKeys.USE_AUTO_DECRYPTION,
                    defaultValue = false,
                    title = stringResource(id = R.string.setting_decrypt_immersive_mod_title),
                    subtitle = stringResource(id = R.string.setting_decrypt_immersive_mod_subtitle),
                )
                AnimatedVisibility(
                    visible = useAutoDecryption,
                    enter = expandVertically(animationSpec = tween(400)) + fadeIn(),
                    exit = shrinkVertically(animationSpec = tween(400)) + fadeOut()
                ) {
                    val decryptionModeOptions = remember {
                        listOf(
                            RadioOption(DECRYPTION_MODE_STANDARD, "标准模式"),
                            RadioOption(DECRYPTION_MODE_IMMERSIVE, "沉浸模式")
                        )
                    }
                    SegmentedButtonSetting(
                        settingKey = SettingKeys.DECRYPTION_MODE,
                        defaultOptionKey = DECRYPTION_MODE_STANDARD,
                        title = stringResource(id = R.string.setting_decryption_mode_info_title),
                        options = decryptionModeOptions,
                        titleExtraContent = {
                            // ✨ 看！之前所有复杂的 TooltipBox 代码，
                            // 现在都变成了这一行极其清晰的调用！
                            InfoDialogIcon(
                                title = stringResource(R.string.setting_decryption_mode_info_text),
                                text = stringResource(R.string.setting_decryption_mode_info_desc),
                                contentDescription = stringResource(R.string.setting_decryption_mode_info_desc)
                            )
                        }
                    )
                }
            }

        }
        Spacer(Modifier.padding(4.dp))
    }
}
