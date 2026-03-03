package me.wjz.nekocrypt.data

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.service.handler.CustomAppHandler

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

//建立一个LocalDataStoreManager的CompositionLocal，专门给ComposeUI用的
val LocalDataStoreManager = staticCompositionLocalOf<DataStoreManager> {
    error("No DataStoreManager provided")
}

/**
 * ✨ [新增] 一个专门用于在Compose上下文中，以State的形式订阅密钥数组变化的Hook。
 *
 * @param initialValue 当Flow还在加载时的初始默认值。
 * @return 一个 State<Array<String>> 对象，它的 .value 会随着DataStore的变化而自动更新。
 */
@Composable
fun rememberKeyArrayState(initialValue: Array<String> = emptyArray()): State<Array<String>> {
    val dataStoreManager = LocalDataStoreManager.current
    return dataStoreManager.getKeyArrayFlow().collectAsState(initial = initialValue)
}


/**
 * ✨ [新增] 一个专门用于在Compose上下文中，以State的形式订阅customApp变化的Hook。
 *
 * @param initialValue 当Flow还在加载时的初始默认值。
 * @return 一个 State<Array<String>> 对象，它的 .value 会随着DataStore的变化而自动更新。
 */
@Composable
fun rememberCustomAppListState(initialValue: List<CustomAppHandler> = emptyList()): State<List<CustomAppHandler>> {
    val dataStoreManager = LocalDataStoreManager.current
    return dataStoreManager.getCustomAppsFlow().collectAsState(initial = initialValue)
}


class DataStoreManager(private val context: Context) {

    //通用的读取方法 (使用泛型)
    fun <T> getSettingFlow(key: Preferences.Key<T>, defaultValue: T): Flow<T> {
        return context.dataStore.data.map { preferences ->
            preferences[key] ?: defaultValue
        }.catch { exception -> throw exception }
    }

    //提供一个一次性的读取方法
    suspend fun <T> readSetting(key: Preferences.Key<T>, defaultValue: T): T {
        // .first() 是一个来自 kotlinx-coroutines-core 的魔法，
        // 它会等待 Flow 发射第一个值，然后就返回，不再继续监听。
        return getSettingFlow(key, defaultValue).first()
    }

    //通用的写入方法
    suspend fun <T> saveSetting(key: Preferences.Key<T>, value: T) {
        context.dataStore.edit { preferences -> preferences[key] = value }
    }

    /**
     * (可选) 通用的清除单个设置的方法
     */
    suspend fun <T> clearSetting(key: Preferences.Key<T>) {
        context.dataStore.edit { preferences -> preferences.remove(key) }
    }

    /**
     * (可选) 清除所有设置的方法
     */
    suspend fun clearAllSettings() {
        context.dataStore.edit { preferences -> preferences.clear() }
    }

    /**
     * 保存密钥数组。
     * 调用者只需要传入一个数组，无需关心JSON转换的细节。
     */
    suspend fun saveKeyArray(keys: Array<String>) {
        val jsonString = Json.encodeToString(keys)
        saveSetting(SettingKeys.ALL_THE_KEYS, jsonString)
    }

    /**
     * 获取密钥数组，用于后台的上下文。
     */
    fun getKeyArrayFlow(): Flow<Array<String>> {
        return getSettingFlow(SettingKeys.ALL_THE_KEYS, "[]").map { jsonString ->
            if (jsonString.isEmpty()) arrayOf(Constant.DEFAULT_SECRET_KEY)
            else {
                try {
                    val keys = Json.decodeFromString<Array<String>>(jsonString)
                    if (keys.isEmpty()) arrayOf(Constant.DEFAULT_SECRET_KEY) else keys
                } catch (e: Exception) {
                    Log.e("Neko", "解析密钥数组失败!", e)
                    arrayOf(Constant.DEFAULT_SECRET_KEY) //解析失败返回默认值
                }
            }
        }
    }

    /**
     * 保存自定义应用列表。
     * 追加形式保存
     */
    suspend fun addCustomApp(newApp: CustomAppHandler) {
        // 1. 读取当前的列表
        val currentApps = getCustomAppsFlow().first().toMutableList()
        // 2. 添加新的配置
        currentApps.add(newApp)
        // 3. 将更新后的列表序列化成 JSON 字符串
        val jsonString = Json.encodeToString(currentApps)
        // 4. 保存回 DataStore
        saveSetting(SettingKeys.CUSTOM_APPS, jsonString)
    }

    /**
     * 删除包名对应的自定义handler
     */
    suspend fun deleteCustomApp(packageName:String){
        val currentApps = getCustomAppsFlow().first().toMutableList()
        currentApps.removeAll { it.packageName == packageName }
        val jsonString = Json.encodeToString(currentApps)
        saveSetting(SettingKeys.CUSTOM_APPS, jsonString)
    }

    /**
     * ✨ 新增：获取自定义应用列表的 Flow。
     * 它从 DataStore 读取JSON字符串，并将其反序列化为 CustomAppHandler 列表。
     * 如果解析失败或没有数据，返回一个空列表。
     */
    fun getCustomAppsFlow(): Flow<List<CustomAppHandler>> {
        // "[]" 是一个空的JSON数组，作为安全的默认值
        return getSettingFlow(SettingKeys.CUSTOM_APPS, "[]").map { jsonString ->
            try {
                Json.decodeFromString<List<CustomAppHandler>>(jsonString)
            } catch (e: Exception) {
                Log.e("NekoCrypt", "解析自定义应用列表失败!", e)
                emptyList() // 解析失败时返回空列表
            }
        }
    }

}