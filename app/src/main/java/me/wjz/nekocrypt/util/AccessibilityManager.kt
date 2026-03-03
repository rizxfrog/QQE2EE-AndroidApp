package me.wjz.nekocrypt.util

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.wjz.nekocrypt.util.PermissionUtil.isAccessibilityServiceEnabled

/**
 * 一个 Composable 函数，用于记住并监听无障碍服务的开启状态。
 * 当应用从后台返回前台时（例如，用户在设置页开启权限后返回），它会自动刷新状态。
 *
 * @param context 上下文环境。
 * @param serviceClass 你的无障碍服务的类名，例如：MyAccessibilityService::class.java。
 * @return 一个 State<Boolean> 对象，实时代表着服务是否开启。
 */
@Composable
fun rememberAccessibilityServiceState(
    context: Context,
    serviceClass: Class<out AccessibilityService>
): State<Boolean> {
    val accessibilityState= remember{ mutableStateOf(isAccessibilityServiceEnabled(context)) }
    // 2. 获取当前 Composable 的生命周期所有者
    val lifecycleOwner = LocalLifecycleOwner.current
    // 3. 使用 DisposableEffect 来添加和移除生命周期观察者，防止内存泄漏
    DisposableEffect(lifecycleOwner) {
        // 创建一个观察者
        val observer = LifecycleEventObserver { _, event ->
            // 当生命周期事件为 ON_RESUME (恢复) 时，说明界面回到了前台
            if (event == Lifecycle.Event.ON_RESUME) {
                // 重新检查一次无障碍权限的状态，并更新 state
                accessibilityState.value = isAccessibilityServiceEnabled(context)
            }
        }

        // 将观察者添加到生命周期中
        lifecycleOwner.lifecycle.addObserver(observer)

        // onDispose 会在 Composable 离开屏幕时被调用
        onDispose {
            // 从生命周期中移除观察者，避免内存泄漏
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    // 4. 返回这个 state，UI 可以订阅它的变化
    return accessibilityState
}

/**
 * 创建一个意图(Intent)并跳转到系统的无障碍功能设置页面。
 *
 * @param context 上下文环境。
 */
fun openAccessibilitySettings(context: Context) {
    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
    // 确保在 Activity 栈外启动新的任务
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}
