package me.wjz.nekocrypt.service.handler

class WeChatHandler : BaseChatAppHandler() {
    companion object{
        const val ID_SEND_BTN="com.tencent.mm:id/bql"
        const val ID_INPUT="com.tencent.mm:id/bkk"
        const val ID_MESSAGE_TEXT="com.tencent.mm:id/bkl"
        const val PACKAGE_NAME ="com.tencent.mm"
        const val CLASS_NAME_RECYCLER_VIEW = "com.tencent.mm:id/bp0"
        const val APP_NAME ="微信"
    }

    override val packageName: String
        get() = PACKAGE_NAME

    override val inputId: String
        get() = ID_INPUT

    override val sendBtnId: String
        get() = ID_SEND_BTN

    override val messageTextId: String
        get() = ID_MESSAGE_TEXT

    override val messageListClassName: String
        get() = CLASS_NAME_RECYCLER_VIEW
}