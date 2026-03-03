package me.wjz.nekocrypt.ui.dialog

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.Constant
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.SettingKeys
import me.wjz.nekocrypt.data.LocalDataStoreManager
import me.wjz.nekocrypt.data.rememberKeyArrayState
import me.wjz.nekocrypt.hook.rememberDataStoreState


@Composable
fun KeyManagementDialog(onDismissRequest: () -> Unit) {
    // 状态管理
    val dataStoreManager = LocalDataStoreManager.current
    val coroutineScope = rememberCoroutineScope()

    val keysFromDataStore: Array<String> by rememberKeyArrayState()
    val keys = remember { mutableStateListOf<String>() }

    // 数据库中key改变，同步到compose中
    LaunchedEffect(keysFromDataStore) {
        if(keys.toList()!=keysFromDataStore.toList()){
            keys.clear()
            keys.addAll(keysFromDataStore)
        }
    }

    // 当前正在使用的密钥
    var activeKey by rememberDataStoreState(SettingKeys.CURRENT_KEY, Constant.DEFAULT_SECRET_KEY)
    // 我们不再需要 showAddKeyDialog，而是用一个新的状态来控制“添加模式”
    var isAddingNewKey by remember { mutableStateOf(false) }

    var keyToDelete by remember { mutableStateOf<String?>(null) }
    val clipboardManager = LocalClipboardManager.current

    fun saveKeys(updatedKeys: SnapshotStateList<String>) {
        coroutineScope.launch {
            dataStoreManager.saveKeyArray(updatedKeys.toTypedArray())
        }
    }

    Dialog(
        onDismissRequest = {
            onDismissRequest()
        },
        // ✨ 2. [核心修正] 告诉Dialog不要使用平台默认的窄宽度
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.90f), // ✨ 设置宽度为屏幕可用宽度的90%
            shape = RoundedCornerShape(24.dp),
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = stringResource(R.string.key_management_dialog_key_management),
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn(
                    modifier = Modifier.heightIn(max = LocalConfiguration.current.screenHeightDp * 0.5.dp),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 密钥列表item
                    items(keys) { key ->
                        KeyItem(
                            keyText = key,
                            isActive = key == activeKey,
                            onSetAsActive = { activeKey = key },
                            onCopy = { clipboardManager.setText(AnnotatedString(key)) },
                            onDelete = { keyToDelete = key }
                        )
                    }
                    // 密钥添加item
                    item {
                        AnimatedVisibility(visible = isAddingNewKey) {
                            KeyEditItem( // ✨ 命名已更新
                                onAddKey = { newKey ->
                                    if (newKey.isNotBlank() && !keys.contains(newKey)) {
                                        keys.add(newKey)
                                        saveKeys(keys)
                                    }
                                    isAddingNewKey = false // 完成后关闭输入模式
                                }
                            )
                        }
                    }

                    // +号按钮，用来添加密钥。
                    item {
                        AnimatedVisibility(visible = !isAddingNewKey) {
                            AddNewKeyButton(onClick = { isAddingNewKey = true })
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(onClick = onDismissRequest) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        }
    }

    if (keyToDelete != null) {
        DeleteConfirmDialog(
            onDismiss = { keyToDelete = null },
            onConfirm = {
                keys.remove(keyToDelete)
                if (activeKey == keyToDelete && keys.isNotEmpty()) {
                    activeKey = keys.first()
                }
                // ✨ 5. [核心] 删除后，立刻保存
                saveKeys(keys)
                keyToDelete = null
            }
        )
    }
}

/**
 * 列表里的单个密钥UI
 */
@Composable
private fun KeyItem(
    keyText: String,
    isActive: Boolean,
    onSetAsActive: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit
){
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(24.dp),
        border = if (isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
        colors = CardDefaults.cardColors(
            containerColor = if(isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)
        )
    ){
        Row(
            modifier = Modifier
                .clickable(onClick = onSetAsActive)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Icon(
                imageVector = Icons.Default.VpnKey,
                contentDescription = "密钥图标",
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = keyText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal
            )
            AnimatedVisibility(
                visible = isActive,
                enter = scaleIn() + fadeIn(),
                exit = fadeOut()
            ) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = "当前活动密钥",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onCopy) {
                Icon(Icons.Outlined.ContentCopy, contentDescription = "复制密钥",tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "删除密钥", tint = MaterialTheme.colorScheme.secondary)
            }
        }
    }
}

@Composable
private fun AddNewKeyButton(onClick: () -> Unit) {
    val shape = RoundedCornerShape(32.dp)
    Card(
        modifier = Modifier
            .fillMaxWidth(0.25f)
            .padding(vertical = 4.dp)
            .clip(shape)
            .clickable(onClick = onClick),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f)
        ),
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = stringResource(R.string.key_management_dialog_key_add),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun KeyEditItem(
    onAddKey: (String) -> Unit
) {
    var newKeyText by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val elevation by animateDpAsState(targetValue = 6.dp, label = "")
    // 当输入框出现时，自动请求焦点
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.secondary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        // ✨ 核心修正：我们用一个普通的 TextField，并把它变成“隐形”的！
        TextField(
            value = newKeyText,
            onValueChange = { newKeyText = it },
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester)
                .onFocusChanged { focusState ->
                    // 当输入框失去焦点时，自动保存
                    if (!focusState.isFocused && newKeyText.isNotBlank()) {
                        onAddKey(newKeyText)
                    }
                },
            // 用 placeholder 感觉比 label 更适合这里的UI
            placeholder = { Text(stringResource(R.string.key_screen_new_key_placeholder)) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                onAddKey(newKeyText)
                focusManager.clearFocus() // 清除焦点，触发 onFocusChanged
            }),
            // ✨ 魔法在这里！我们把所有背景和边框颜色都设置为透明！
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            )
        )
    }
}

/**
 * 删除确认对话框
 */
@Composable
private fun DeleteConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("确认删除") },
        text = { Text("确定要删除这个密钥吗？此操作不可撤销。") },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Text("删除")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        }
    )
}