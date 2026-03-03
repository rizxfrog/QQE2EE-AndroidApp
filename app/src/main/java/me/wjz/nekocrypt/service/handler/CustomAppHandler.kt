package me.wjz.nekocrypt.service.handler

import kotlinx.serialization.Serializable

/**
 * 一个数据类，用于表示用户自定义的应用配置。
 * @Serializable 注解是必须的，它告诉 kotlinx.serialization 库这个类可以被转换成JSON。
 */
@Serializable
data class CustomAppHandler(
    // 需要重写 ChatAppHandler 接口中的所有属性
    override val packageName: String,
    override val inputId: String,
    override val sendBtnId: String,
    override val messageTextId: String,
    override val messageListClassName: String

) : BaseChatAppHandler()
