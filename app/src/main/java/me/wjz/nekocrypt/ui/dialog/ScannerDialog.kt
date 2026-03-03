package me.wjz.nekocrypt.ui.dialog

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.ui.activity.FoundNodeInfo
import me.wjz.nekocrypt.ui.activity.MessageListScanResult
import me.wjz.nekocrypt.ui.activity.ScanResult

/**
 * 用于封装用户最终选择的节点信息的数据类。
 */
data class ScanSelections(
    val inputNode: FoundNodeInfo,
    val sendBtnNode: FoundNodeInfo,
    val messageList: FoundNodeInfo,
    val messageText: FoundNodeInfo
)

/**
 * 悬浮扫描按钮点击后显示的对话框 Composable (V3 协同版)。
 */
@Composable
fun ScannerDialog(
    scanResult: ScanResult,
    onDismissRequest: () -> Unit,
    onConfirm: (ScanSelections,ScanResult) -> Unit
) {
    val configuration = LocalConfiguration.current
    val screenHeight = configuration.screenHeightDp.dp
    //  记住用户的选择
    var selectedInput by remember { mutableStateOf<FoundNodeInfo?>(null) }
    var selectedSendBtn by remember { mutableStateOf<FoundNodeInfo?>(null) }
    var selectedList by remember { mutableStateOf<MessageListScanResult?>(null) }
    var selectedMessageText by remember { mutableStateOf<FoundNodeInfo?>(null) }

    //  helpDialog
    var showHelpDialog by remember { mutableStateOf(false) }
    // --- 2. 衍生状态：只有当所有选项都选了，确认按钮才能点击 ---
    val isConfirmEnabled by remember(selectedInput, selectedSendBtn, selectedList, selectedMessageText) {
        derivedStateOf {
            selectedInput != null && selectedSendBtn != null && selectedList != null && selectedMessageText != null
        }
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f) // 使用屏幕宽度的90%
                .padding(16.dp)
                .heightIn(max = screenHeight * 0.85f), // 最大高度为屏幕的85%
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // --- 核心修改：总标题和一个可点击的帮助图标 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.scanner_dialog_title),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    IconButton(onClick = { showHelpDialog = true }) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = "instruction"
                        )
                    }
                }

                // 显示当前应用的包名和名称
                Text(
                    text = "${scanResult.name} (${scanResult.packageName})",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                // 使用可滚动的 LazyColumn 来展示所有区块
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    item {
                        SelectableSection(
                            title = stringResource(R.string.scanner_dialog_section_input),
                            nodes = scanResult.foundInputNodes,
                            selectedNode = selectedInput,
                            onNodeSelected = {
                                selectedInput = if (selectedInput == it) null else it
                            }
                        )
                    }
                    item {
                        SelectableSection(
                            title = stringResource(R.string.scanner_dialog_section_send_btn),
                            nodes = scanResult.foundSendBtnNodes,
                            selectedNode = selectedSendBtn,
                            onNodeSelected = { selectedSendBtn = if (selectedSendBtn == it) null else it}
                        )
                    }
                    item {
                        MessageListSelectionSection(
                            title = stringResource(R.string.scanner_dialog_section_msg_list),
                            lists = scanResult.foundMessageLists,
                            selectedList = selectedList,
                            selectedText = selectedMessageText,
                            onListSelected = {
                                selectedList = if(selectedList == it) null else it

                                selectedMessageText = null // ✨ 切换列表时，重置消息文本的选择
                            },
                            onTextSelected = { selectedMessageText =if(selectedMessageText == it)null else it }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- 底部按钮 ---
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            // ✨ 点击确认时，把所有选择打包成 ScanSelections 并回传
                            onConfirm(
                                ScanSelections(
                                    inputNode = selectedInput!!,
                                    sendBtnNode = selectedSendBtn!!,
                                    messageList = selectedList!!.listContainerInfo,
                                    messageText = selectedMessageText!!
                                ),scanResult
                            )
                        },
                        enabled = isConfirmEnabled // ✨ 绑定按钮的可用状态
                    ) {
                        Text(stringResource(R.string.accept))
                    }
                }
            }
        }
    }

    if (showHelpDialog) {
        ScannerHelpDialog(onDismissRequest = { showHelpDialog = false })
    }
}

/**
 * ✨ 全新：用于选择“消息列表”和“消息文本”的复合区块
 */
@Composable
private fun MessageListSelectionSection(
    title: String,
    lists: List<MessageListScanResult>,
    selectedList: MessageListScanResult?,
    selectedText: FoundNodeInfo?,
    onListSelected: (MessageListScanResult) -> Unit,
    onTextSelected: (FoundNodeInfo) -> Unit
) {
    if (lists.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 12.dp)) {
            Text(text = "$title (${lists.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            lists.forEach { listResult ->
                // 1. 先把每个列表容器作为可选项显示
                SelectableNodeInfoCard(
                    nodeInfo = listResult.listContainerInfo,
                    isSelected = listResult == selectedList,
                    onSelected = { onListSelected(listResult) }
                )

                // 2. 如果当前列表被选中了，就“展开”它内部的消息文本作为下一级选项

                Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
                    Text(
                        text = "└─ ${stringResource(R.string.scanner_dialog_section_msg_text)} (${listResult.messageTexts.size})",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    // ✨ 核心修改：只有当父列表被选中时，才动态展示详细的子节点卡片
                    AnimatedVisibility(visible = listResult == selectedList) {
                        Column {
                            listResult.messageTexts.forEach { textNode ->
                                SelectableNodeInfoCard(
                                    nodeInfo = textNode,
                                    isSelected = textNode == selectedText,
                                    onSelected = { onTextSelected(textNode) }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * ✨ 全新：通用的、用于单选的区块（用于输入框和发送按钮）
 */
@Composable
private fun SelectableSection(
    title: String,
    nodes: List<FoundNodeInfo>,
    selectedNode: FoundNodeInfo?,
    onNodeSelected: (FoundNodeInfo) -> Unit
) {
    if (nodes.isNotEmpty()) {
        Column(modifier = Modifier.padding(bottom = 16.dp)) {
            Text(text = "$title (${nodes.size})", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            nodes.forEach { node ->
                SelectableNodeInfoCard(
                    nodeInfo = node,
                    isSelected = node == selectedNode,
                    onSelected = { onNodeSelected(node) }
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * ✨ 全新：带单选按钮的节点信息卡片
 */
@Composable
private fun SelectableNodeInfoCard(
    nodeInfo: FoundNodeInfo,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .selectable(
                selected = isSelected,
                onClick = onSelected,
                role = Role.RadioButton
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                if (nodeInfo.resourceId?.isNotBlank()==true) InfoRow(label = stringResource(R.string.scanner_dialog_card_id), value = nodeInfo.resourceId)
                InfoRow(label = stringResource(R.string.scanner_dialog_card_class), value = nodeInfo.className)
                if (nodeInfo.text?.isNotBlank()==true) InfoRow(label = stringResource(R.string.scanner_dialog_card_text), value = nodeInfo.text)
                if (nodeInfo.contentDescription?.isNotBlank()==true) InfoRow(label = stringResource(R.string.scanner_dialog_card_desc), value = nodeInfo.contentDescription)
            }
        }
    }
}

/**
 * 用于在卡片内显示一行“标签: 内容”信息，并支持点击复制。
 */
@Composable
private fun InfoRow(label: String, value: String) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val hasCopyHint = stringResource(R.string.has_copy)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 标签
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(48.dp) // 给标签一个固定宽度，让内容对齐
        )
        // 内容
        Surface(
            onClick = {
                if (value.isNotEmpty()) {
                    clipboardManager.setText(AnnotatedString(value))
                    Toast.makeText(context, "'$value' $hasCopyHint", Toast.LENGTH_SHORT).show()
                }
            },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.5f)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ScannerHelpDialog(onDismissRequest: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Rounded.Info, contentDescription = null) },
        title = { Text(text = stringResource(R.string.scanner_help_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.scanner_help_dialog_intro))
                Text(stringResource(R.string.scanner_help_dialog_instruction))
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.accept))
            }
        }
    )
}