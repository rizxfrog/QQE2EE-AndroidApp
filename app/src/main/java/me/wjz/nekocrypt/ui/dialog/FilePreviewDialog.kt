package me.wjz.nekocrypt.ui.dialog

import android.net.Uri
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.FilePresent
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme
import me.wjz.nekocrypt.util.NCFileProtocol
import me.wjz.nekocrypt.util.NCFileType
import me.wjz.nekocrypt.util.formatFileSize

/**
 * ✨ 全新改造的文件详情对话框 UI
 */
@Composable
fun FilePreviewDialog(
    fileInfo: NCFileProtocol,
    downloadProgress: Int?,
    downloadedFileUri: Uri?,
    isImageSavedThisTime : Boolean,
    onDismissRequest: () -> Unit,
    onDownloadRequest: (NCFileProtocol) -> Unit,
    onOpenRequest: (Uri) -> Unit,
    onSaveToGalleryRequest: (Uri) -> Unit // ✨ 新增：保存到相册的回调
) {
    val coroutineScope = rememberCoroutineScope()
    var isVisible by remember { mutableStateOf(false) }
    val isDownloading = downloadProgress != null // ✨ 判断当前是否正在下载

    // 出现动画，并且判断如果是图片且未缓存，直接下载。
    LaunchedEffect(Unit) {
        isVisible = true
        if(fileInfo.type == NCFileType.IMAGE && downloadedFileUri == null){
            onDownloadRequest(fileInfo)
        }
    }

    // 带动画的关闭逻辑
    fun dismissWithAnimation() {
        coroutineScope.launch {
            isVisible = false
            delay(300) // 等待动画播放完毕
            onDismissRequest()
        }
    }

    NekoCryptTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(16.dp)) {
            AnimatedVisibility(
                visible = isVisible,
                enter = scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(animationSpec = tween(200)),
                exit = scaleOut(animationSpec = tween(300)) + fadeOut(
                    animationSpec = tween(300)
                )
            ) {
                Card(
                    modifier = Modifier
                        .shadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp)),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally)
                    {
                        // 标题
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if(fileInfo.type == NCFileType.FILE) stringResource(R.string.dialog_download_file_file_info) else fileInfo.name,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // 核心预览区
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 600.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(contentColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            AnimatedContent(
                                targetState = downloadedFileUri,
                                label = "preview_animation",
                            ) { uri ->
                                if(uri !=null && fileInfo.type == NCFileType.IMAGE){
                                    // 如果是图片并且已经有缓存文件，直接展示图片
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = "image preview",
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(2.dp)),
                                        contentScale = ContentScale.FillWidth
                                    )
                                } else {
                                    // 图片下载未完成，或者根本不是图片格式，就展示普通的样式
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center
                                    ){
                                        if (isDownloading) DownloadProgressIndicator(progress = downloadProgress)
                                        else InitialFileInfo(fileInfo = fileInfo)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // --- ✨ 智能操作按钮 ---
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            // 关闭按钮
                            TextButton(onClick = { dismissWithAnimation() }, enabled = !isDownloading) {
                                Text(stringResource(R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))

                            AnimatedContent(
                                targetState = downloadedFileUri != null,
                                transitionSpec = {
                                    fadeIn(animationSpec = tween(200)) togetherWith
                                            fadeOut(animationSpec = tween(200))
                                },
                                label = "ActionButtonAnimation"
                            ) { isDownloaded ->
                                if (isDownloaded) {
                                    // --- 已下载完成 ---
                                    if (fileInfo.type == NCFileType.FILE) {
                                        // 文件类型：显示“打开文件”
                                        Button(onClick = {
                                            // ✨ 核心修正2：先调用打开逻辑，再触发带动画的关闭
                                            onOpenRequest(downloadedFileUri!!)
                                            dismissWithAnimation()
                                        }) {
                                            Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(stringResource(R.string.open_file))
                                        }
                                    } else {
                                        // 图片类型：显示带动画的“保存”或“已保存”按钮
                                        Button(
                                            onClick = { onSaveToGalleryRequest(downloadedFileUri!!) },
                                            enabled = !isImageSavedThisTime
                                        ) {
                                            AnimatedContent(
                                                targetState = isImageSavedThisTime,
                                                transitionSpec = {
                                                    fadeIn(tween(300)) togetherWith fadeOut(tween(300))
                                                },
                                                label = "SaveButtonAnimation"
                                            ) { saved ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    if (saved) {
                                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(stringResource(R.string.image_saved_to_gallery_saved))
                                                    } else {
                                                        Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(stringResource(R.string.save_to_gallery))
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    // --- 未下载 ---
                                    Button(onClick = { onDownloadRequest(fileInfo) }, enabled = !isDownloading) {
                                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(stringResource(R.string.download))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}



/**
 * ✨ 改造后的初始文件信息布局
 * 用于显示大图标、文件名和大小。
 */
@Composable
private fun InitialFileInfo(fileInfo: NCFileProtocol) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        val icon = when(fileInfo.type){
            NCFileType.FILE -> Icons.Default.FilePresent
            NCFileType.IMAGE -> Icons.Default.Image
        }

        Icon(
            imageVector = icon,
            contentDescription = "文件类型",
            modifier = Modifier.size(96.dp), // 变大了！
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 文件名
        Text(
            text = fileInfo.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(8.dp))
        // 文件大小
        Text(
            text = fileInfo.size.formatFileSize(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun DownloadProgressIndicator(progress: Int?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val animatedProgress by animateFloatAsState(
            targetValue = (progress ?: 0) / 100f,
            animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
            label = "download_progress_animation"
        )
        CircularProgressIndicator(progress = { animatedProgress })
        Spacer(Modifier.height(16.dp))
        Text(
            text = "${stringResource(R.string.downloading)} ${progress ?: 0}%",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}