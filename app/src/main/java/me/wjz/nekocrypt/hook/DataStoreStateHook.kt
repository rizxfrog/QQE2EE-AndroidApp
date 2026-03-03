package me.wjz.nekocrypt.hook

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.preferences.core.Preferences
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.data.DataStoreManager
import me.wjz.nekocrypt.data.LocalDataStoreManager
import kotlin.reflect.KProperty

class DataStoreStateDelegate<T>(
    private val state: State<T>,
    private val scope: CoroutineScope,
    private val saver: suspend (T) -> Unit
){
    /**
     * `operator fun getValue`
     * 当你读取属性时（如 `if (isChecked)`），Kotlin 会调用这个函数。
     * 我们只需返回内部 `State` 的当前值。
     */
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        return state.value
    }

    /**
     * `operator fun setValue`
     * 当你写入属性时（如 `isChecked = false`），Kotlin 会调用这个函数。
     * 我们在这里启动一个协程，调用 `saver` lambda 将新值保存到 DataStore。
     */
    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        scope.launch {
            saver(value)
        }
    }
}

@Composable
fun <T> rememberDataStoreState(
    key: Preferences.Key<T>,
    defaultValue: T
): DataStoreStateDelegate<T> {
    // 1. 获取全局唯一的 DataStoreManager 和协程作用域
    val dataStoreManager: DataStoreManager = LocalDataStoreManager.current
    val scope: CoroutineScope = rememberCoroutineScope()
    //2. 从Flow里面拿数据并转化成Compose的State
    val state: State<T> = dataStoreManager.getSettingFlow(key, defaultValue)
        .collectAsStateWithLifecycle(initialValue = defaultValue)

    // 用remember来创建并记住委托类实例
    return remember(dataStoreManager, scope, key) {
        DataStoreStateDelegate(
            state = state,
            scope = scope,
            saver = { newValue -> dataStoreManager.saveSetting(key, newValue) }
        )
    }
}