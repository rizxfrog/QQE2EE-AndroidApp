package me.wjz.nekocrypt.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * 专门用于下载并解密文件的工具类
 */
object CryptoDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()


    suspend fun download(
        fileInfo: NCFileProtocol,
        targetFile: File, // ✨ 接收一个目标文件
        onProgress: (Int) -> Unit,
    ): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = fileInfo.url
                val encryptionKey = fileInfo.encryptionKey

                val request = Request.Builder().url(url).build()

                // execute 会 suspend
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("下载失败，响应码: ${response.code}")

                    ProgressInputStream(response.body.byteStream()) { bytesRead ->
                        // 这里处理下载回调
                        val estimatedTotalRead = (bytesRead - CryptoUploader.SINGLE_PIXEL_GIF_BUFFER.size).coerceAtLeast(0)
                        val progress = (estimatedTotalRead * 100 / fileInfo.size).toInt()
                        onProgress(progress.coerceIn(0, 100))

                    }.use { networkStream ->
                        // 使用循环确保跳过完整的GIF头
                        val skipSize = CryptoUploader.SINGLE_PIXEL_GIF_BUFFER.size.toLong()
                        var skipped = 0L
                        while (skipped < skipSize) {
                            // 这里用while循环是因为这个skip可能会跳过少于预期的字节数量。
                            val n = networkStream.skip(skipSize - skipped)
                            if (n <= 0) throw IOException("无法跳过GIF头部，文件可能已损坏。")
                            skipped += n
                        }
                        // 流式解密 && 下载
                        targetFile.outputStream().use { outputStream ->
                            CryptoManager.decryptStream(networkStream, outputStream, encryptionKey)
                        }
                    }
                }
                targetFile
            }
        }
}

/**
 * 用来追踪 InputStream读取进度的辅助类
 * 它通过包裹一个现有的 InputStream，来监听数据的读取过程。
 */
class ProgressInputStream(
    inStream: InputStream,
    private val onProgress: (Long) -> Unit,
) : FilterInputStream(inStream) {

    private var bytesRead: Long = 0 // 已经读取的字节数

    /**
     * 只重写这一个 read 方法就足够了。
     * 因为单字节的 read() 在内部会自动调用这个方法，
     * 这样可以避免重复计算进度。
     */
    override fun read(b: ByteArray, off: Int, len: Int): Int {
        val read = super.read(b, off, len)
        if (read > 0) {
            bytesRead += read
            // 安全地调用监听器，报告当前的进度
            onProgress(bytesRead)
        }
        return read
    }
}