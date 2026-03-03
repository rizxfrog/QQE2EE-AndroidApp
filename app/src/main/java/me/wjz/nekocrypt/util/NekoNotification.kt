package me.wjz.nekocrypt.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.app.NotificationCompat
import me.wjz.nekocrypt.R

// 用于创建通知的对象
object NekoNotification {

    const val NEKO_NOTIFICATION_ID = 20040821
    private const val CHANNEL_ID = "NekoCryptKeepAlive"
    private const val CHANNEL_NAME = "NekoCrypt 服务状态"

    /**
     * 创建通知渠道（仅在Android 8.0+需要）。
     */
    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_MIN // 设置为最低重要性，用户不会被打扰
        ).apply {
            description = "用于保持NekoCrypt服务在后台稳定运行"
            setShowBadge(false) // 不在桌面图标上显示角标
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    /**
     * 构建前台服务的常驻通知。
     */
    fun build(context: Context): Notification {
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle("NekoCrypt 正在守护中")
            .setContentText("加密服务正在后台运行...")
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ✨ 请确保你有一个图标资源
            .setPriority(NotificationCompat.PRIORITY_MIN) // 设置为最低优先级
            .setOngoing(true) // 设置为常驻通知，用户无法划掉，被划掉了会被降级。
            .build()
    }
}