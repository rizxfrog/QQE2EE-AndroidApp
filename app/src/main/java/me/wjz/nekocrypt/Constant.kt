package me.wjz.nekocrypt

import androidx.annotation.StringRes
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import me.wjz.nekocrypt.service.handler.ChatAppHandler
import me.wjz.nekocrypt.service.handler.QQHandler
import me.wjz.nekocrypt.service.handler.WeChatHandler

object Constant {
    const val APP_NAME = "NekoCrypt"
    const val DEFAULT_SECRET_KEY = "20040821"//You know what it means...

    // ---- 其他 ----
    const val EDIT_TEXT="EditText"
    const val VIEW_ID_BTN = "Button"

    //  扫描intent额外字段的key
    const val SCAN_RESULT = "scan_result"
}

object SettingKeys {
    val CURRENT_KEY = stringPreferencesKey("current_key")
    // 用 String 类型的 Key 来存储序列化后的密钥数组
    val ALL_THE_KEYS = stringPreferencesKey("all_the_keys")
    val USE_AUTO_ENCRYPTION = booleanPreferencesKey("use_auto_encryption")
    val USE_AUTO_DECRYPTION = booleanPreferencesKey("use_auto_decryption")
    val SCAN_BTN_ACTIVE = booleanPreferencesKey("scan_btn_active")
    val ENCRYPTION_MODE = stringPreferencesKey("encryption_mode")
    val DECRYPTION_MODE = stringPreferencesKey("decryption_mode")
    // 标准加密模式下，长按时间设置
    val ENCRYPTION_LONG_PRESS_DELAY = longPreferencesKey("encryption_long_press_delay")
    // 标准解密模式下，悬浮窗的显示时间设置
    val DECRYPTION_WINDOW_SHOW_TIME = longPreferencesKey("decryption_window_show_time")
    // 沉浸式解密下密文弹窗位置更新间隔
    val DECRYPTION_WINDOW_POSITION_UPDATE_DELAY = longPreferencesKey("decryption_window_position_update_delay")
    // 按钮遮罩的颜色
    val SEND_BTN_OVERLAY_COLOR = stringPreferencesKey("send_btn_overlay_color")
    // 控制弹出发送图片or文件视图的双击最大间隔时间
    val SHOW_ATTACHMENT_VIEW_DOUBLE_CLICK_THRESHOLD = longPreferencesKey("show_attachment_view_double_click_threshold")
    val CUSTOM_APPS = stringPreferencesKey("custom_apps")
    //  当前密文风格
    val CIPHERTEXT_STYLE = stringPreferencesKey("ciphertext_style")
    // 存储风格文本的最小和最大词语数
    val CIPHERTEXT_STYLE_LENGTH_MIN = intPreferencesKey("ciphertext_style_length_min")
    val CIPHERTEXT_STYLE_LENGTH_MAX = intPreferencesKey("ciphertext_style_length_max")
}

object CommonKeys {
    const val ENCRYPTION_MODE_STANDARD = "standard"
    const val ENCRYPTION_MODE_IMMERSIVE = "immersive"
    const val DECRYPTION_MODE_STANDARD = "standard"
    const val DECRYPTION_MODE_IMMERSIVE = "immersive"
}

object AppRegistry {
    /**
     * 包含所有受支持应用处理器实例的权威列表。
     * 未来要支持新的App，只需要在这里新增一行即可！
     * UI 和 Service 都会从这里读取信息。
     */
    val allHandlers: List<ChatAppHandler> = listOf(
        QQHandler(),
        WeChatHandler()
        //  TelegramHandler(),
        //  ... 以后在这里添加更多
    )
}

enum class CryptoMode(val key: String, @StringRes val labelResId: Int){
    STANDARD("standard", R.string.mode_standard),
    IMMERSIVE("immersive", R.string.mode_immersive);

    companion object {
        /**
         * 一个辅助函数，可以根据存储的 key 安全地找回对应的枚举实例。
         * 如果找不到，就返回一个默认值。
         */
        fun fromKey(key: String?): CryptoMode {
            // entries 是一个由编译器自动生成的属性，包含了枚举的所有实例
            return entries.find { it.key == key } ?: STANDARD
        }
    }
}