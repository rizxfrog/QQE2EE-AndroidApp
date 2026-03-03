package me.wjz.nekocrypt.service.handler

import android.view.accessibility.AccessibilityEvent
import com.dianming.phoneapp.MyAccessibilityService


/**
 * 聊天应用处理器的通用接口。
 * 定义了所有受支持的聊天应用都需要提供的基本信息和逻辑。
 */
interface ChatAppHandler {
    /**
     * 该处理器对应的应用包名。
     */
    val packageName: String

    /**
     * 聊天界面输入框的资源ID。
     */
    val inputId: String

    /**
     * 聊天界面发送按钮的资源ID。
     */
    val sendBtnId: String

    /**
     * 气泡消息的ID
     */
    val messageTextId: String

    /**
     * 存放消息列表的className，QQ的这个class无ID，则不提供
     */
    val messageListClassName: String

    /**
     * 当该处理器被激活时调用（例如，用户打开了对应的App）。
     * @param service 无障碍服务的实例，用于获取上下文、协程作用域等。
     */
    fun onHandlerActivated(service: MyAccessibilityService)

    /**
     * 当该处理器被停用时调用（例如，用户离开了对应的App）。
     */
    fun onHandlerDeactivated()

    /**
     * 处理该应用相关的无障碍事件。
     * @param event 接收到的事件。
     * @param service 无障碍服务的实例。
     */
    fun onAccessibilityEvent(event: AccessibilityEvent, service: MyAccessibilityService)
}