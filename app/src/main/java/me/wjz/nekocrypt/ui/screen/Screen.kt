package me.wjz.nekocrypt.ui.screen

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import me.wjz.nekocrypt.R

/**
 * App的所有页面来源。
 */
sealed class Screen (
    val route: String,
    @StringRes val titleResId: Int,
    val icon: ImageVector,
    val content: @Composable () -> Unit
){
    //  主页
    data object Home : Screen(
        route = "home",
        titleResId = R.string.home,
        icon = Icons.Outlined.Home,
        content = { HomeScreen() }
    )
    // 加解密页
    data object Crypto : Screen(
        route = "crypto",
        titleResId = R.string.crypto,
        icon = Icons.Outlined.Lock,
        content = { CryptoScreen() }
    )

    // 密钥管理页
    data object Key : Screen(
        route = "key",
        titleResId = R.string.key,
        icon = Icons.Outlined.Key,
        content = { KeyScreen() }
    )

    // 设置页
    data object Setting : Screen(
        route = "setting",
        titleResId = R.string.settings,
        icon = Icons.Outlined.Settings,
        content = { SettingsScreen() }
    )
    companion object {
        val allScreens: List<Screen> = listOf(Home, Crypto, Key, Setting)
    }
}