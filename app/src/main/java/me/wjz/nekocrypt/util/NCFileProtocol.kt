package me.wjz.nekocrypt.util

import android.util.Log
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import me.wjz.nekocrypt.util.CryptoManager.applyCiphertextStyle
import org.json.JSONException

enum class NCFileType{
    IMAGE,FILE;
}

const val NC_FILE_PROTOCOL_PREFIX = "NCFile://"

@Serializable
data class NCFileProtocol(
    val url: String,
    val size: Long,
    val name: String,
    val type: NCFileType,
    val encryptionKey: String
) {
    companion object {
        /**
         * ✨ [反序列化] (使用Fastjson)
         * @return 如果解密和解析成功，返回NCFileProtocol对象；否则返回null。
         */
        fun fromString(decryptedString: String): NCFileProtocol? {
            return try {
                if (!decryptedString.startsWith(NC_FILE_PROTOCOL_PREFIX)) return null

                val jsonPayload = decryptedString.substringAfter(NC_FILE_PROTOCOL_PREFIX)
                // ✨ 使用Fastjson进行解析
                Json.decodeFromString<NCFileProtocol>(jsonPayload)
            } catch (e: JSONException) {
                // Fastjson解析失败
                null
            } catch (e: Exception) {
                // 捕获其他所有可能的异常（如枚举转换失败）
                null
            }
        }
    }

    /**
     * ✨ [加密 & 序列化] (使用Fastjson)
     * 将当前的NCFileProtocol对象，转换为一个完整的、加密的协议字符串。
     * @param encryptionKey 用于加密的密钥。
     * @return 格式为 "NCFile://[加密并隐写编码后的JSON载荷]" 的字符串。
     */
    fun toEncryptedString(encryptionKey: String): String {
        Log.d("NekoAccessibility", "protocol本身结果结果：$this")
        val payloadJson = Json.encodeToString(this)
        Log.d("NekoAccessibility", "protocol转json结果：$payloadJson")
        return CryptoManager.encrypt(NC_FILE_PROTOCOL_PREFIX + payloadJson, encryptionKey).applyCiphertextStyle()
    }
}