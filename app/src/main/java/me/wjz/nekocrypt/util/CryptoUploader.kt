package me.wjz.nekocrypt.util

import android.net.Uri
import android.util.Base64
import android.util.Log
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import me.wjz.nekocrypt.NekoCryptApp
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import java.io.IOException
import java.io.InputStream
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import kotlin.coroutines.resumeWithException

/**
 * 封装图片、视频和文件的加密上传逻辑
 */
object CryptoUploader {
    // 这个接口只支持20M内的图片 & mp4
    private const val UPLOAD_URL =
        "https://chatbot.weixin.qq.com/weixinh5/webapp/pfnYYEumBeFN7Yb3TAxwrabYVOa4R9/cos/upload"

    // 从你的JS代码转换过来的1像素GIF的ByteArray
    val SINGLE_PIXEL_GIF_BUFFER: ByteArray =
        Base64.decode("R0lGODdhAQABAIABAP///wAAACwAAAAAAQABAAACAkQBADs=", Base64.DEFAULT)

    private val client = OkHttpClient.Builder()
        .connectTimeout(1, TimeUnit.MINUTES)
        .readTimeout(5, TimeUnit.MINUTES)
        .writeTimeout(5, TimeUnit.MINUTES)
        .build()


    /**
     * ✨ [终极形态] 支持流式上传的主函数
     * 它直接接收Uri，内部处理所有文件IO和加密，内存安全。
     * @param uri 文件的Uri地址
     * @param fileName 文件名
     * @param encryptionKey 加密密钥
     * @param onProcess 进度回调 (0-100)
     * @return 服务器返回的响应字符串
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun upload(
        uri: Uri,
        fileName: String,
        encryptionKey: String,
        onProcess: (progress: Int) -> Unit,
    ): NCFileProtocol {
        Log.d(NekoCryptApp.TAG, "准备上传文件: $uri，文件名：$fileName，大小${getFileSize(uri)}")

        val inputStream = NekoCryptApp.instance.contentResolver.openInputStream(uri)
            ?: throw IOException("Failed to open input stream from URI")

        val requestBody = StreamingProcessRequestBody(
            inputStream = inputStream,
            fileSize = getFileSize(uri),
            encryptionKey = encryptionKey,
            onProcess = onProcess
        )

        val multipartBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("media", "$fileName.gif", requestBody).build()

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
            )
            .post(multipartBody)
            .build()

        // 发送我们的请求
        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)

            // 当协程被取消时，我们也取消网络请求
            continuation.invokeOnCancellation {
                call.cancel()
                inputStream.close()
            }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    // 确保协程还在活跃状态
                    if (continuation.isActive) {
                        continuation.resumeWithException(e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        response.use { res ->
                            val bodyString = res.body.string()
                            if (res.isSuccessful && bodyString.isNotBlank()) {
                                try {
                                    // ✨ 1. 将字符串转换为JSON对象
                                    val jsonObject = Json.parseToJsonElement(bodyString).jsonObject

                                    // ✨ 2. 从JSON对象中提取"url"字段的值
                                    val fileUrl = jsonObject["url"]?.jsonPrimitive?.content

                                    // ✨ 3. [可选，但推荐] 检查一下URL是不是空的
                                    if (fileUrl?.isNotBlank() == true) {
                                        // 拿到URL后，就可以封装成对象恢复协程了
                                        continuation.resume(
                                            NCFileProtocol(
                                                url = fileUrl,
                                                size = getFileSize(uri),
                                                name =fileName,
                                                encryptionKey =encryptionKey,
                                                type = if(isFileImage(uri)) NCFileType.IMAGE else NCFileType.FILE
                                            ), null
                                        )
                                    } else {
                                        continuation.resumeWithException(IOException("服务器返回的JSON中url为空。"))
                                    }

                                } catch (e: Exception) {
                                    // 如果bodyString不是一个有效的JSON，或者没有"url"字段，就会在这里捕获到异常
                                    continuation.resumeWithException(
                                        IOException(
                                            "解析服务器响应JSON失败。",
                                            e
                                        )
                                    )
                                }
                            } else {
                                continuation.resumeWithException(
                                    IOException("上传失败，响应码: ${res.code}，响应体: $response")
                                )
                            }
                        }
                    }
                }
            })
        }
    }

    /**
     * ✨ [新增重载] 直接上传内存中的字节数组。
     * 适用于处理已经存在于内存中的数据。
     * @param fileBytes 文件的完整字节内容
     * @param fileName 文件名
     * @param encryptionKey 加密密钥
     * @param onProcess 进度回调 (0-100)
     * @return url 返回的结果是直接的url下载地址，未经过包装
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun upload(
        fileBytes: ByteArray,
        fileName: String = "",
        encryptionKey: String,
        onProcess: (progress: Int) -> Unit,
    ): NCFileProtocol {
        val encryptedBytes = CryptoManager.encrypt(fileBytes, encryptionKey)
        val payload = SINGLE_PIXEL_GIF_BUFFER + encryptedBytes
        val requestBody = ProcessRequestBody(payload, "image/gif".toMediaTypeOrNull(), onProcess)

        val multipartBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("media", "$fileName.gif", requestBody)
            .build()

        val request = Request.Builder()
            .url(UPLOAD_URL)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36"
            )
            .post(multipartBody)
            .build()

        return suspendCancellableCoroutine { continuation ->
            val call = client.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }

            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    if (continuation.isActive) continuation.resumeWithException(e)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (continuation.isActive) {
                        response.use { res ->
                            val bodyString = res.body.string()
                            if (res.isSuccessful && bodyString.isNotBlank()) {
                                try {
                                    // ✨ 1. 将字符串转换为JSON对象
                                    val jsonObject = Json.parseToJsonElement(bodyString).jsonObject

                                    // ✨ 2. 从JSON对象中提取"url"字段的值
                                    val fileUrl = jsonObject["url"]?.jsonPrimitive?.content

                                    // ✨ 3. [可选，但推荐] 检查一下URL是不是空的
                                    if (fileUrl?.isNotBlank() == true) {
                                        // 拿到URL后，就可以封装成对象恢复协程了
                                        continuation.resume(
                                            NCFileProtocol(
                                                url = fileUrl,
                                                size = fileBytes.size.toLong(),
                                                name =fileName,
                                                encryptionKey =encryptionKey,
                                                type = if(fileBytes.isImage()) NCFileType.IMAGE else NCFileType.FILE
                                            ), null
                                        )
                                    } else {
                                        continuation.resumeWithException(IOException("服务器返回的JSON中url为空。"))
                                    }

                                } catch (e: Exception) {
                                    // 如果bodyString不是一个有效的JSON，或者没有"url"字段，就会在这里捕获到异常
                                    continuation.resumeWithException(
                                        IOException(
                                            "解析服务器响应JSON失败。",
                                            e
                                        )
                                    )
                                }
                            } else {
                                continuation.resumeWithException(
                                    IOException("上传失败，响应码: ${res.code}，响应体: $response")
                                )
                            }
                        }
                    }
                }
            })
        }
    }
}


/**
 * 现在换成支持流式加密和上传的RequestBody
 */
private class StreamingProcessRequestBody(
    private val inputStream: InputStream,
    private val fileSize: Long,
    private val encryptionKey: String,
    private val onProcess: (Int) -> Unit,
) : RequestBody() {

    override fun contentLength(): Long =
        -1  // 给okHttp的这里返回-1，就可以让它自动切换到分块传输编码，Chunked Transfer Encoding。

    override fun contentType(): MediaType? = "image/gif".toMediaTypeOrNull()

    // 这里面完成先加密后再写入
    override fun writeTo(sink: BufferedSink) {
        // 先写入伪装头
        sink.write(CryptoUploader.SINGLE_PIXEL_GIF_BUFFER)
        // 准备GCM加密
        val iv = ByteArray(CryptoManager.IV_LENGTH_BYTES)
        SecureRandom().nextBytes(iv)    //填充随机内容
        // 直接把我们生成的iv写入进去
        sink.write(iv)

        // 下面开始创建加密工具
        val cipher = Cipher.getInstance(CryptoManager.TRANSFORMATION)
        val parameterSpec = GCMParameterSpec(CryptoManager.TAG_LENGTH_BITS, iv)
        cipher.init(
            Cipher.ENCRYPT_MODE,
            CryptoManager.deriveKeyFromString(encryptionKey),
            parameterSpec
        )

        // 开始流式读写和加密
        var originalBytesRead = 0L
        val buffer = ByteArray(8 * 1024) // 8KB的buffer
        var bytesRead: Int  // 已经读取的bytes的大小，如果是-1就说明读完了，不然大部分就是buffer大小

        inputStream.use {
            // 这里要注意，also里面的it，就是also前执行的返回值，在这里就是读取的bytes数。
            while (it.read(buffer).also { bytesRead = it } != -1) {
                originalBytesRead += bytesRead
                // 加密当前读取到的数据块
                val encryptedBuffer = cipher.update(buffer, 0, bytesRead)
                if (encryptedBuffer != null) sink.write(encryptedBuffer) // 发送加密好的片段

                // 计算上传进度
                val progress = (originalBytesRead / fileSize * 100).coerceAtMost(100).toInt()
                onProcess(progress)
            }
            // 到这里，数据就读取完了，还要调用doFinal，插入GCM的hash
            val encryptedBuffer = cipher.doFinal()
            if (encryptedBuffer != null) sink.write(encryptedBuffer)
        }
    }
}

// 一个普通的上传body，带进度条回调
private class ProcessRequestBody(
    private val data: ByteArray,
    private val contentType: MediaType?,
    private val onProcess: (Int) -> Unit,
) : RequestBody() {
    override fun contentType(): MediaType? = contentType
    override fun contentLength(): Long = data.size.toLong()

    override fun writeTo(sink: BufferedSink) {
        val totalBytes = contentLength()
        var bytesWritten = 0L
        val bufferSize = 4 * 1024 // 每次读取4KB
        // 用use方法，可以自动关闭，类似python open with
        data.inputStream().use { inputStream ->
            val buffer = ByteArray(bufferSize) // 创建临时缓冲区
            var read: Int
            /**
             * 逻辑：read放入一个数组，stream流会自动更新放入的这个buffer，接着，它的返回值是本次读取到的值，刚开始会是buffer的
             * 大小，我们这里是4096，读取完了再读取，就会返回-1，刚好结束循环。
             */
            while (inputStream.read(buffer).also { read = it } != -1) {
                sink.write(buffer, 0, read)
                bytesWritten += read
                val progress = (100 * bytesWritten / totalBytes).toInt()
                onProcess(progress)
            }
        }
    }
}

// 下面是其中一个回调示例

//{
//    "url":"https://abc.aa.com.123123.gif",
//    "filekey":"openaiassets_1754798953_12333333213.gif",
//    "err":null,
//    "code":0
//}