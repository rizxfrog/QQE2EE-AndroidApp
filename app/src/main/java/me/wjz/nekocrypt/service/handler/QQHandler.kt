package me.wjz.nekocrypt.service.handler

/**
 * 针对 QQ 的具体处理器实现。
 */
class QQHandler : BaseChatAppHandler() {
    companion object{
        const val ID_SEND_BTN="com.tencent.mobileqq:id/send_btn"
        const val ID_INPUT="com.tencent.mobileqq:id/input"
        // 某些版本ID_MESSAGE_TEXT是SQB
        const val ID_MESSAGE_TEXT="com.tencent.mobileqq:id/sbl"
        const val PACKAGE_NAME ="com.tencent.mobileqq"
        const val APP_NAME ="QQ"
        const val CLASS_NAME_RECYCLER_VIEW="RecyclerView"
    }

    override val packageName: String get() = PACKAGE_NAME
    override val inputId: String get() = ID_INPUT

    override val sendBtnId: String get() = ID_SEND_BTN

    override val messageTextId: String get() = ID_MESSAGE_TEXT
    override val messageListClassName: String get() = CLASS_NAME_RECYCLER_VIEW
}