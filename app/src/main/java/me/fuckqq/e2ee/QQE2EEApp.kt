package me.fuckqq.e2ee

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import me.fuckqq.e2ee.data.DataStoreManager

class QQE2EEApp : Application() {
    // 在 Application 创建时，我们懒加载地创建 DataStoreManager 的实例。
    // 它只会被创建一次！
    val dataStoreManager: DataStoreManager by lazy {
        DataStoreManager(this)
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel() // 创建通知渠道，用于在 Android 8.0 及以上版本上显示通知
        instance = this
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            dataStoreManager.ensureDefaultKeyInitialized()
        }
        Log.d(TAG, "QQE2EEApp onCreate")
    }


    companion object {
        const val SERVICE_CHANNEL_ID = "QQE2EEServiceChannel"
        const val TAG = "QQE2EE"
        lateinit var instance: QQE2EEApp private set
    }

    private fun createNotificationChannel() {
        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            getString(R.string.notification_title),
            NotificationManager.IMPORTANCE_LOW // 使用较低的重要性，避免打扰用户
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(serviceChannel)
    }
}
