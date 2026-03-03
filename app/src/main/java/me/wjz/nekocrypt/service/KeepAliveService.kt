package me.wjz.nekocrypt.service

import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.util.NekoNotification

/**
 * ✨ 一个专门用于保活的前台服务。
 * 它的唯一职责就是通过一个常驻通知，告诉系统我们的App正在运行重要任务。
 */
class KeepAliveService : Service() {
    // 保活窗口
    private var keepAliveOverlay: View? = null
    private val windowManager by lazy { getSystemService(WINDOW_SERVICE) as WindowManager }
    companion object {
        private const val TAG = NekoCryptApp.TAG

        // ✨ 提供一个标准的启动方法，方便外部调用
        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            context.startService(intent)
        }

        // ✨ 提供一个标准的停止方法
        fun stop(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            context.stopService(intent)
        }
    }

    private fun createKeepAliveOverlay() {
        if (keepAliveOverlay != null) return
        keepAliveOverlay = View(this)
        val layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        val params = WindowManager.LayoutParams(
            0, 0, 0, 0, layoutFlag,
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
        try {
            windowManager.addView(keepAliveOverlay, params)
            Log.d(TAG, "“保活”悬浮窗创建成功！")
        } catch (e: Exception) {
            Log.e(TAG, "创建“保活”悬浮窗失败", e)
        }
    }

    private fun removeKeepAliveOverlay() {
        keepAliveOverlay?.let {
            try {
                windowManager.removeView(it)
                Log.d(TAG, "“保活”悬浮窗已移除。")
            } catch (e: Exception) {
                // 忽略窗口已经不存在等异常
            } finally {
                keepAliveOverlay = null
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "保活服务已启动。")
        // 1. 创建通知渠道（在Android 8.0及以上版本是必需的）
        NekoNotification.createChannel(this)

        // 2. 创建一个通知
        val notification = NekoNotification.build(this)

        // 3. ✨ 最关键的一步：将服务推到前台！
        //    第一个参数是一个唯一的通知ID，第二个参数是我们创建的通知。
        startForeground(NekoNotification.NEKO_NOTIFICATION_ID, notification)
        // 我们同时创一个保活悬浮窗
        createKeepAliveOverlay()
        // START_STICKY 表示如果服务被系统意外杀死，系统会尝试重新启动它
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        removeKeepAliveOverlay()
        Log.d(TAG, "保活服务已销毁，保活悬浮窗已销毁。")
        stopForeground(true)
    }

    /**
     * ✨ 实现 onBind 方法。
     * 因为我们这是一个启动服务（Started Service），而不是绑定服务（Bound Service），
     * 所以我们不需要处理绑定逻辑，直接返回 null 即可。
     */
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}