package me.wjz.nekocrypt.ui.dialog

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Collections
import androidx.compose.material.icons.outlined.FileOpen
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.activity.AttachmentPickerActivity
import me.wjz.nekocrypt.ui.theme.NekoCryptTheme

// ✨ 1. 将UI状态数据类聚合并定义在这里
data class AttachmentState(
    var progress: Float? = null,
    var previewInfo: AttachmentPreviewState? = null,
    var result: String = "", // NCFileProtocol的格式
) {
    // 计算属性，方便在UI逻辑中使用
    val isUploading: Boolean get() = progress != null
    val isUploadFinished: Boolean get() = result.isNotEmpty()
}

// 预览信息的具体内容
data class AttachmentPreviewState(
    var uri: Uri,
    var fileName: String,
    var fileSizeFormatted: String,
    var isImage: Boolean,
    val imageAspectRatio: Float? = null, // 新增：图片的宽高比
)

/**
 * ✨ [最终精致版] 发送附件的对话框UI内容
 * 带有动画、进度反馈，并合并了图片/视频选项。
 */
@Composable
fun SendAttachmentDialog(
    // ✨ 2. 接收聚合后的状态对象
    attachmentState: AttachmentState,
    onDismissRequest: () -> Unit,
    onSendRequest: (String) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()

    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isVisible = true }


    fun dismissWithAnimation() {
        coroutineScope.launch {
            isVisible = false
            delay(300)
            onDismissRequest()
        }
    }

    NekoCryptTheme(darkTheme = false) {
        Box(modifier = Modifier.padding(8.dp)) {
            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn(animationSpec = tween(200)) + scaleIn(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ),
                exit = fadeOut(animationSpec = tween(300)) + scaleOut(animationSpec = tween(300))
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = stringResource(R.string.crypto_attachment_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(24.dp))

                        Box(contentAlignment = Alignment.Center) {
                            // 封装好的组件，包含图片和文件按钮。
                            AttachmentOptions(
                                isUploading = attachmentState.isUploading,
                                onClicked = { dismissWithAnimation() } // 这里点击关闭对话框，稍后再重新拉起。
                            )
                            // 下面就是加载态的圆圈加载
                            Row(horizontalArrangement = Arrangement.Center) {
                                AnimatedVisibility(
                                    visible = attachmentState.isUploading,
                                    enter = fadeIn(animationSpec = tween(300)),
                                    exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                                        animationSpec = tween(
                                            200
                                        )
                                    )
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(
                                            progress = { attachmentState.progress ?: 0f }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = stringResource(R.string.crypto_attachment_uploading),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }


                        Spacer(modifier = Modifier.height(16.dp))

                        // ✨ 3. 预览区域的逻辑更新
                        AnimatedVisibility(
                            // 上传完毕才显示
                            visible = attachmentState.previewInfo != null && attachmentState.result.isNotEmpty()
                        ) {
                            // 使用 rememberUpdatedState 可以在不引起整个对话框重组的情况下更新预览内容
                            val currentPreview by rememberUpdatedState(attachmentState.previewInfo)
                            currentPreview?.let {
                                FilePreview(
                                    uri = it.uri,
                                    fileName = it.fileName,
                                    fileSize = it.fileSizeFormatted,
                                    isImage = it.isImage,
                                    aspectRatio = it.imageAspectRatio
                                )
                            }
                        }

                        // ✨ 4. URL输入框已完全移除

                        Spacer(modifier = Modifier.height(24.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            // 取消按钮
                            TextButton(
                                onClick = { dismissWithAnimation() },
                                enabled = !attachmentState.isUploading
                            ) {
                                Text(stringResource(R.string.cancel))
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                // ✨ 发送按钮的可用性也由外部状态决定
                                onClick = { onSendRequest(attachmentState.result) },
                                enabled = attachmentState.isUploadFinished && !attachmentState.isUploading
                            ) {
                                Text(stringResource(R.string.send))
                            }
                        }
                    }
                }
            }
        }
    }
}

// 文件和图片的预览组件
@Composable
fun FilePreview(
    uri: Uri,
    fileName: String,
    fileSize: String,
    isImage: Boolean,
    aspectRatio: Float?, // 新增宽高比参数
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 80.dp), // 设置一个最小高度
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(
                alpha = 0.3f
            )
        )
    ) {
        if (isImage) {
            // 如果是图片，使用AsyncImage来异步加载并显示
            AsyncImage(
                model = uri,
                contentDescription = "Image Preview",
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .let {
                        // ✨ 关键改动：如果宽高比有效，就应用它
                        if (aspectRatio != null && aspectRatio > 0) {
                            it.aspectRatio(aspectRatio)
                        } else {
                            // 否则给一个默认高度
                            it.height(180.dp)
                        }
                    }
                    .clip(RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop // 裁剪图片以填充空间
            )
        } else {
            // 如果是普通文件，显示图标、文件名和大小
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = "File Icon",
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2, // 最多显示两行
                        overflow = TextOverflow.Ellipsis // 超出部分显示省略号
                    )
                    Text(
                        text = fileSize,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentOptions(
    isUploading: Boolean,
    modifier: Modifier = Modifier,
    onClicked: () -> Unit,
) {
    val context = LocalContext.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SendOptionItem(
            icon = Icons.Outlined.Collections,
            label = stringResource(R.string.crypto_attachment_media),
            enabled = !isUploading,
            onClick = {
//                val intent = Intent(Intent.ACTION_PICK).apply {
//                    type = "image/* video/*" // 同时选择图片和视频
//                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//                }
//                context.startActivity(intent)

                // 这里体现了两种intent的创建方式，上面的是先弹出一个弹窗，让用户选择其一，再具体拉出对应弹窗，适合service上下文

                // 下面我们这里指定一个activity，就方便很多。
                val intent = Intent(context, AttachmentPickerActivity::class.java).apply {
                    // 这里放个extra，activity内部就根据这个额外的kv判断具体拉起逻辑
                    putExtra(
                        AttachmentPickerActivity.EXTRA_PICK_TYPE,
                        AttachmentPickerActivity.TYPE_MEDIA
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                onClicked()
            }
        )
        SendOptionItem(
            icon = Icons.Outlined.FileOpen,
            label = stringResource(R.string.crypto_attachment_file),
            enabled = !isUploading,
            onClick = {
                val intent = Intent(context, AttachmentPickerActivity::class.java).apply {
                    putExtra(
                        AttachmentPickerActivity.EXTRA_PICK_TYPE,
                        AttachmentPickerActivity.TYPE_FILE
                    )
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                onClicked()
            }
        )
    }
}

/**
 * 对话框里可点击的选项按钮的UI封装
 */
@Composable
private fun RowScope.SendOptionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val alpha by animateFloatAsState(targetValue = if (enabled) 1f else 0.5f, label = "")
    val shape = RoundedCornerShape(12.dp)
    Surface(
        modifier = modifier
            .weight(1f)
            .clip(shape)
            .clickable(
                onClick = onClick,
                enabled = enabled,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f * alpha),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f * alpha))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
            )
        }
    }
}
