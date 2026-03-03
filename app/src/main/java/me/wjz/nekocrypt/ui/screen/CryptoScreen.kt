package me.wjz.nekocrypt.ui.screen

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Notes
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.wjz.nekocrypt.Constant.DEFAULT_SECRET_KEY
import me.wjz.nekocrypt.NekoCryptApp
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys.CIPHERTEXT_STYLE
import me.wjz.nekocrypt.SettingKeys.CURRENT_KEY
import me.wjz.nekocrypt.data.rememberKeyArrayState
import me.wjz.nekocrypt.hook.rememberDataStoreState
import me.wjz.nekocrypt.ui.dialog.FilePreviewDialog
import me.wjz.nekocrypt.ui.dialog.KeyManagementDialog
import me.wjz.nekocrypt.util.CiphertextStyleType
import me.wjz.nekocrypt.util.CryptoDownloader
import me.wjz.nekocrypt.util.CryptoManager
import me.wjz.nekocrypt.util.CryptoManager.applyCiphertextStyle
import me.wjz.nekocrypt.util.CryptoManager.containsCiphertext
import me.wjz.nekocrypt.util.NCFileProtocol
import me.wjz.nekocrypt.util.getCacheFileFor
import me.wjz.nekocrypt.util.getUriForFile
import java.io.IOException

@Composable
fun CryptoScreen(modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    var isEncryptMode by remember { mutableStateOf(true) }//当前是加密or解密
    //  获取当前密钥，用于加密。没有就是默认密钥
    val secretKey: String by rememberDataStoreState(CURRENT_KEY, DEFAULT_SECRET_KEY)
    //  这里还要拿密钥列表，for循环遍历解密
    val secretKeyList by rememberKeyArrayState()

    //  当前的密文风格类型
    var ciphertextStyleType by rememberDataStoreState(CIPHERTEXT_STYLE, CiphertextStyleType.NEKO.toString())

    val decryptFailed = stringResource(id = R.string.crypto_decrypt_fail)//解密错误的text。
    var isDecryptFailed by remember { mutableStateOf(false) }
    // 新增：用于统计的状态
    var charCount by remember { mutableIntStateOf(0) }
    var elapsedTime by remember { mutableLongStateOf(0L) }
    // 新增一个状态，用来控制密钥管理对话框的显示和隐藏
    var showKeyDialog by remember { mutableStateOf(false) }

    //  管理文件弹窗和下载相关
    var fileInfoToShow by remember { mutableStateOf<NCFileProtocol?>(null) }
    var downloadProgress by remember { mutableStateOf<Int?>(null) }
    var downloadedFileUri by remember { mutableStateOf<Uri?>(null) }
    var isImageSavedThisTime by remember { mutableStateOf(false) }

    //自动加解密
    LaunchedEffect(inputText, secretKey) {
        if (inputText.isEmpty()) {
            outputText = ""
            fileInfoToShow = null // 清空文件信息
            charCount = 0
            elapsedTime = 0L
            return@LaunchedEffect
        }

        val startTime = System.currentTimeMillis()
        var ciphertextCharCount = 0

        // 先判断是不是密文
        if (inputText.containsCiphertext()) {
            isEncryptMode = false
            ciphertextCharCount = inputText.length
            var decryptedText:String? = null
            //  执行解密
            for( key in secretKeyList){
                decryptedText = CryptoManager.decrypt(inputText, key)
                if(decryptedText!=null) break
            }

            // 再判断解密后的内容是不是文件协议
            val fileInfo = decryptedText?.let { NCFileProtocol.fromString(it) }

            if (fileInfo != null) {
                // --- 是文件！准备显示弹窗 ---
                outputText = "" // 清空普通文本输出
                isDecryptFailed = false
                // 检查文件是否已缓存
                val targetFile = getCacheFileFor(context, fileInfo)
                if (targetFile.exists() && targetFile.length() == fileInfo.size) {
                    downloadedFileUri = getUriForFile(context, targetFile)
                    downloadProgress = null
                } else {
                    downloadedFileUri = null
                    downloadProgress = null
                }
                isImageSavedThisTime = false // 重置保存状态
                fileInfoToShow = fileInfo // ✨ 触发弹窗显示！
            } else {
                // --- 是普通文本 ---
                fileInfoToShow = null // 确保文件弹窗不显示
                isDecryptFailed = decryptedText == null
                outputText = decryptedText ?: decryptFailed
            }
        } else {
            // --- 是原文，执行加密 ---
            isEncryptMode = true
            fileInfoToShow = null
            val ciphertext = CryptoManager.encrypt(inputText, secretKey).applyCiphertextStyle()
            ciphertextCharCount = ciphertext.length
            outputText = ciphertext
        }

        val endTime = System.currentTimeMillis()
        elapsedTime = endTime - startTime
        charCount = ciphertextCharCount
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 1. 密钥选择器
        KeySelector(
            selectedKeyName = secretKey,
            onClick = {
                // 当点击时，将状态设置为true，以显示对话框
                showKeyDialog = true
            }
        )
        Spacer(modifier = Modifier.height(16.dp))

        CiphertextStyleSelector(
            selectedStyle = CiphertextStyleType.fromName(ciphertextStyleType),
            onStyleSelected = { newStyle ->
                // ✨ 核心修正：直接赋值即可！
                // 我们的 DataStoreStateDelegate 会自动处理保存逻辑和UI更新。
                ciphertextStyleType = newStyle.toString()
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 2. 输入文本框
        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            modifier = Modifier
                .fillMaxWidth()
//                .height(180.dp),这里不设置固定高度
                ,
            minLines = 1,//控制默认的最小行数
            maxLines = 6,//控制最大行数
            label = { Text(stringResource(id = R.string.crypto_input_label)) },
            placeholder = { Text(stringResource(id = R.string.crypto_input_placeholder)) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Rounded.Notes,
                    contentDescription = stringResource(id = R.string.crypto_input_icon_desc)
                )
            },
            // 右方的辅助按钮
            trailingIcon = {
                Row {
                    // 粘贴按钮
                    IconButton(onClick = {
                        clipboardManager.getText()?.text?.let { inputText += it }//这里+=
                        Toast.makeText(
                            context,
                            context.getString(R.string.crypto_pasted_from_clipboard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.ContentPaste,
                            contentDescription = stringResource(id = R.string.crypto_paste_icon_desc)
                        )
                    }
                    // 清空按钮，仅在有输入时显示
                    AnimatedVisibility(visible = inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(
                                Icons.Rounded.Clear,
                                contentDescription = stringResource(id = R.string.crypto_clear_input_icon_desc)
                            )
                        }
                    }
                }
            },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 输出结果区域
        // 使用 AnimatedVisibility，当有输出时，这个区域会平滑地淡入
        AnimatedVisibility(
            visible = outputText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(500)),
            exit = fadeOut(animationSpec = tween(500))
        ) {
            OutlinedTextField(
                value = outputText,
                onValueChange = {}, // 输出框通常是只读的
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth(),
//                    .height(180.dp),
                minLines = 1,
                maxLines = 6,
                isError = isDecryptFailed,
                label = { Text(stringResource(if (isEncryptMode) R.string.crypto_result_label_encrypted else R.string.crypto_result_label_decrypted)) },
                // 右下角的复制按钮
                trailingIcon = {
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(outputText))
                        Toast.makeText(
                            context,
                            context.getString(R.string.crypto_copied_to_clipboard),
                            Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = stringResource(id = R.string.crypto_copy_result_icon_desc)
                        )
                    }
                },
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.secondary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 统计信息区域
        AnimatedVisibility(
            visible = outputText.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            CryptoStats(
                charCount = charCount,
                elapsedTime = elapsedTime
            )
        }

        if(showKeyDialog){
            KeyManagementDialog(onDismissRequest = { showKeyDialog = false })
        }

        //  加上文件展示dialog
        fileInfoToShow?.let { fileInfo ->
            val scope= rememberCoroutineScope()
            FilePreviewDialog(
                fileInfo = fileInfo,
                downloadProgress = downloadProgress,
                downloadedFileUri = downloadedFileUri,
                isImageSavedThisTime = isImageSavedThisTime,
                onDismissRequest = { fileInfoToShow = null },
                onDownloadRequest = { info ->
                    // 在协程中启动下载
                    scope.launch {
                        val targetFile = getCacheFileFor(context, info)
                        downloadProgress = 0
                        val result = CryptoDownloader.download(
                            fileInfo = info,
                            targetFile = targetFile,
                            onProgress = { progress -> downloadProgress = progress }
                        )
                        if (result.isSuccess) {
                            downloadedFileUri = getUriForFile(context, result.getOrThrow())
                        } else {
                            Toast.makeText(context, "下载失败: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                        }
                        downloadProgress = null
                    }
                },
                onOpenRequest = { uri -> openFile(context, scope,uri, fileInfo) },
                onSaveToGalleryRequest = { uri ->
                    scope.launch { isImageSavedThisTime = saveImageToGallery(context, uri, fileInfo) }
                }
            )
        }
    }
}


/**
 * 一个用于展示统计信息（字符数和耗时）的组件。
 */
@Composable
private fun CryptoStats(
    charCount: Int,
    elapsedTime: Long,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // 无阴影，更轻量
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 字符总数统计
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.crypto_stats_char_count),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = charCount.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // 处理耗时统计
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(id = R.string.crypto_stats_time_elapsed),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.crypto_stats_time_ms, elapsedTime),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}

/**
 * 一个用于展示和选择密钥的自定义组件。
 */
@Composable
fun KeySelector(
    selectedKeyName: String,
    onClick: () -> Unit
) {
    // 将形状定义为一个变量，方便复用
    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Key,
                    contentDescription = stringResource(id = R.string.crypto_key_icon_desc),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(id = R.string.crypto_current_key_label),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f)
                    )
                    Text(
                        text = selectedKeyName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = stringResource(id = R.string.crypto_select_key_icon_desc),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun openFile(context: Context, scope: CoroutineScope, uri: Uri, fileInfo: NCFileProtocol){
    scope.launch {
        try{
            // 1. ✨ 从原始文件名中获取文件后缀
            val extension = fileInfo.name.substringAfterLast('.', "")
            // 2. ✨ 使用 MimeTypeMap 将后缀转换为标准的MIME类型
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension.lowercase())
                ?: "*/*" // 如果找不到，使用通用类型

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri,mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)

        } catch (e: Exception) {
            Log.e(NekoCryptApp.TAG, "打开文件失败", e)
            Toast.makeText(context,context.getString(R.string.cannot_open_file),Toast.LENGTH_SHORT).show()
        }
    }
}

private suspend fun saveImageToGallery(context: Context,uri: Uri, fileInfo: NCFileProtocol): Boolean {

    val success = withContext(Dispatchers.IO) {
        runCatching {
            val extension = fileInfo.name.substringAfterLast('.', "")
            val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)

            // ContentValues 就像一个“档案袋”，我们把新文件的所有信息（元数据）都放进去。
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileInfo.name)      // 文件在相册里显示的名字。
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)         // 文件的mime类型
                // 档案3 & 4 (仅限 Android 10 及以上)：
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    // 告诉系统要把这个文件放在公共的“相册”文件夹里。
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
                    // 先把文件标记为“待定”状态。这意味着在文件内容被完全写入之前，
                    // 其他应用（包括相册自己）是看不到这个文件的，可以防止出现损坏的半成品文件。
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            // 用我们写好的信息，去申请一个URI
            val imageUri = context.contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues
            )
                ?: throw IOException("无法在相册中创建新文件。")

            // 使用我们新的imageUri，写入文件
            context.contentResolver.openOutputStream(imageUri).use { outputStream ->
                context.contentResolver.openInputStream(uri).use { inputStream ->
                    requireNotNull(inputStream) { "无法打开缓存文件的输入流" }
                    requireNotNull(outputStream) { "无法打开相册文件的输出流" }
                    inputStream.copyTo(outputStream)
                }
            }

            // (仅限 Android 10 及以上) 文件内容已经写完，我们再次更新档案，
            // 把“待定”状态改为0，正式通知系统：“文件已准备就绪，可以对外展示了！”
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                context.contentResolver.update(imageUri, contentValues, null, null)
            }

            //顺利完成，返回true
            true
        }.onFailure { e ->
            Log.e(NekoCryptApp.TAG, "保存图片到相册失败", e)
            false // 返回失败
        }.getOrDefault(false) // 拿不到，默认就返回false
    }
    if (success) Toast.makeText(context,context.getString(R.string.image_saved_to_gallery_success),Toast.LENGTH_SHORT).show()
    else Toast.makeText(context,context.getString(R.string.image_saved_to_gallery_failed),Toast.LENGTH_SHORT).show()
    return success
}

/**
 * ✨ 全新：一个用于选择密文伪装风格的下拉菜单组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CiphertextStyleSelector(
    selectedStyle: CiphertextStyleType,
    onStyleSelected: (CiphertextStyleType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    // 从枚举中获取所有可选的样式
    val styles = remember { CiphertextStyleType.entries }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = stringResource(id = selectedStyle.displayNameResId),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.crypto_style_selector_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor() //  这是让下拉菜单能正确定位到输入框的关键！
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
        )
        // 真正的下拉菜单
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            styles.forEach { style ->
                DropdownMenuItem(
                    text = { Text(stringResource(id = style.displayNameResId)) },
                    onClick = {
                        onStyleSelected(style)
                        expanded = false // 选择后收起菜单
                    }
                )
            }
        }
    }
}
