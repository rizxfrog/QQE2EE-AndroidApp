package me.wjz.nekocrypt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import me.wjz.nekocrypt.data.LocalDataStoreManager
import me.wjz.nekocrypt.ui.MainMenu
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme
import me.wjz.nekocrypt.util.PermissionGuard

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()//让App可以上下扩展到最顶端和最低端
        //这是从传统 Android 视图系统切换到 Jetpack Compose 世界的“传送门”！
        // 一旦调用了它，你就可以在这个大括号 {} 里面，用我们之前学过的 @Composable 函数来描绘你的 App 界面了。
        setContent {
            //这里不要在Compose UI中直接引用dataStoreManager，而是在这里注入一个，这样可以方便替换不同的manager，解耦方便复用
            val app = application as NekoCryptApp
            NekoCryptTheme {
                //  权限检查
                PermissionGuard {
                    CompositionLocalProvider(LocalDataStoreManager provides app.dataStoreManager) {
                        MainMenu()
                    }
                }

            }
        }
    }
}
