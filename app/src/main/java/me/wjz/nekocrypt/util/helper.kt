package me.wjz.nekocrypt.util

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.view.accessibility.AccessibilityNodeInfo
import androidx.compose.ui.graphics.Color
import androidx.core.content.FileProvider
import me.wjz.nekocrypt.NekoCryptApp
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

// 取反色
fun Color.inverse(): Color {
    return Color(
        red = 1.0f - this.red,
        green = 1.0f - this.green,
        blue = 1.0f - this.blue,
        alpha = this.alpha
    )
}

/**
 * 根据uri查询文件大小。-1表示未知。
 */
fun getFileSize(uri: Uri): Long {
    NekoCryptApp.instance.contentResolver.query(
        uri,
        null,                                       // ④ projection = null → 返回所有列
        null, null, null   // ⑤ selection/args/sortOrder 均不需要
    )?.use {
        if (it.moveToFirst()) {
            val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
            return if (it.isNull(sizeIndex)) -1 else it.getLong(sizeIndex)
        }
    }
    return -1
}

/**
 * 获取文件名
 */
fun getFileName(uri: Uri): String {
    var fileName = "unknown"
    NekoCryptApp.instance.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val displayNameColumn = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (displayNameColumn != -1) { // 检查 DISPLAY_NAME 列是否存在
                fileName = cursor.getString(displayNameColumn)
            }
        }
    }
    return fileName
}

/**
 * 检查给定的包名是否属于系统应用。
 * @param packageName 需要检查的应用包名。
 * @return 如果是系统应用或核心应用，则返回 true，否则返回 false。
 */
fun isSystemApp(packageName: String?): Boolean {
    if (packageName.isNullOrBlank()) {
        return false
    }
    // 相册属于前者，文件选择器属于后者。
    return packageName.startsWith("com.android.providers") || packageName.startsWith("com.google.android")
}

/**
 * 格式化文件大小，入参单位为bytes
 */
fun Long.formatFileSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(this.toDouble()) / log10(1024.0)).toInt()
    return String.format(
        Locale.US,
        "%.1f %s",
        this / 1024.0.pow(digitGroups.toDouble()),
        units[digitGroups]
    )
}

// 定义图片文件的常见扩展名
val imageExtensions =
    setOf("jpg", "jpeg", "png", "gif", "bmp", "webp", "heic", "svg", "tiff", "psdng")

fun isFileImage(uri: Uri): Boolean {
    val fileName = getFileName(uri)
    // 获取文件的扩展名
    val extension = fileName.substringAfterLast(".").lowercase()
    // 检查扩展名是否在图片扩展名集合中
    return imageExtensions.contains(extension)
}

private val IMAGE_HEADERS = setOf(
    "FFD8",                 // JPEG
    "89504E47",             // PNG
    "47494638",             // GIF
    "49492A00", "4D4D002A", // TIFF 两种字节序
    "424D",                 // BMP
    "52494646",             // WEBP 的前 4 字节
    "000000"                // ICO
)

fun ByteArray.isImage(): Boolean {
    if (isEmpty()) return false
    // 前 8 字节足够覆盖上面所有魔数
    val prefix = take(8)
        .joinToString("") { "%02X".format(it) }
    return IMAGE_HEADERS.any { prefix.startsWith(it) }
}

/**
 * ✨ [新增] 专门获取图片宽高比的辅助函数
 * 它只解码图片的边界信息，不加载整个图片到内存，因此非常高效。
 * @param uri 图片的Uri
 * @return Float类型的宽高比，如果无法获取则返回null
 */
fun getImageAspectRatio(uri: Uri): Float? {
    val currentService = NekoCryptApp.instance
    return try {
        // 使用 contentResolver 打开输入流
        currentService.contentResolver.openInputStream(uri)?.use { inputStream ->
            // 创建一个 BitmapFactory.Options 对象
            val options = BitmapFactory.Options().apply {
                // 设置 inJustDecodeBounds = true 是关键！
                // 这会告诉解码器只解析图片的元数据（包括尺寸），而不真正加载像素数据到内存。
                inJustDecodeBounds = true
            }
            // 执行解码操作（实际上只解码了边界）
            BitmapFactory.decodeStream(inputStream, null, options)

            // 检查是否成功获取了有效的宽度和高度
            if (options.outWidth > 0 && options.outHeight > 0) {
                // 计算并返回宽高比
                options.outWidth.toFloat() / options.outHeight.toFloat()
            } else {
                // 如果尺寸无效，返回null
                null
            }
        }
    } catch (e: IOException) {
        print(e.stackTraceToString())
        null
    }
}

/**
 * 判断节点是否为空，为空依据：无子节点｜无包名｜无viewIdResourceName
  */
fun AccessibilityNodeInfo.isEmpty(): Boolean {
    return this.packageName == null || this.viewIdResourceName == null
}

//  获取文件缓存
fun getCacheFileFor(context: Context, fileInfo: NCFileProtocol): File{
    // 总是用外部缓存，方便分享之类的操作
    val baseDir = context.externalCacheDir ?: context.cacheDir
    val downloadDir = File(baseDir,"download").apply { mkdirs() }
    // 用唯一文件名，避免重名之类的
    val fileName = fileInfo.name.let { name ->
        val dot = name.lastIndexOf('.')
        if (dot == -1) "${name}-${fileInfo.url.hashCode()}"
        else "${name.substring(0, dot)}-${fileInfo.url.hashCode()}${name.substring(dot)}"
    }
    return File(downloadDir,fileName)
}

//  根据File拿Uri
fun getUriForFile(context: Context, file: File):Uri{
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.provider",  // 这个地方的authority一定要和manifest里面配置的一样
        file
    )
}