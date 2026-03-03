package me.wjz.nekocrypt.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.datastore.preferences.core.Preferences
import kotlinx.coroutines.launch
import me.wjz.nekocrypt.R
import me.wjz.nekocrypt.hook.rememberDataStoreState
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * 这是一个自定义的、用于显示设置分组标题的组件。
 * @param title 要显示的标题文字。
 */
@Composable
fun SettingsHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

/**
 * 这是一个自定义的、带开关的设置项组件。
 * 它内部管理自己的 DataStore 状态，并通过一个验证回调来决定是否要更新状态，
 * 从而避免了在权限不足时开关“闪烁”的问题。
 *
 * @param key 用于在 DataStore 中存取状态的 Key。
 * @param defaultValue 开关的默认值。
 * @param icon 左侧显示的图标。
 * @param title 主标题文字。
 * @param subtitle 副标题（描述性文字）。
 * @param onCheckValidated 一个验证回调。当用户尝试改变开关状态时，会先调用它。
 * 你需要在这个回调里执行权限检查等逻辑，并返回 `true` (允许改变) 或 `false` (阻止改变)。
 * @param onStateChanged 当状态被成功改变后，会调用这个回调。你可以在这里执行发送指令等副作用操作。
 */
@Composable
fun SwitchSettingItem(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onCheckValidated: suspend (Boolean) -> Boolean = { true },
    onStateChanged: (Boolean) -> Unit = {},
) {
    // 1. 组件自己管理自己的状态，从 DataStore 读取和写入
    var isChecked by rememberDataStoreState(key, defaultValue)
    val scope = rememberCoroutineScope()

    // 2. 定义一个统一的状态变更处理器
    val changeHandler = { desiredState: Boolean ->
        scope.launch {
            // 3. 在改变状态前，先调用外部传入的“验证函数”
            val canChange = onCheckValidated(desiredState)
            // 4. 只有“验证函数”返回 true，才真正更新状态
            if (canChange) {
                isChecked = desiredState
                // 5. 状态成功更新后，通知外部
                onStateChanged(desiredState)
            }
            // ✨ 如果 canChange 是 false，这里什么都不做，UI上的开关也就不会动啦！
        }
    }

    // 用Row来水平排列元素
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { changeHandler(!isChecked) } // 点击整行也能触发状态变更
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        //显示图标
        icon()
        // 占一点间距
        Spacer(modifier = Modifier.width(16.dp))
        //用Column来垂直排列主标题和副标题
        Column(modifier = Modifier.weight(1f)) {// weight(1f)让这一列占满所有剩余空间
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle, style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.6f)
            ) // 让副标题颜色浅一点
        }
        Switch(checked = isChecked, onCheckedChange = { changeHandler(it) })
    }
}

@Composable
fun ClickableSettingItem(
    icon: @Composable () -> Unit,
    title: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick) // 设置点击事件
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge)
    }
}


@Composable
fun SwitchSettingCard(
    key: Preferences.Key<Boolean>,
    defaultValue: Boolean,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onCheckedChanged: (Boolean) -> Unit = {},
) {
    var isChecked by rememberDataStoreState(key, defaultValue)
    // 将形状定义为一个变量，方便复用
    val cardShape = RoundedCornerShape(16.dp)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable {
                isChecked = !isChecked
                onCheckedChanged(isChecked)
            },
        shape = cardShape,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 16.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            // 开关的状态直接绑定到我们内部的 isChecked 变量
            Switch(
                checked = isChecked,
                onCheckedChange = {
                    isChecked = it
                    onCheckedChanged(it)
                }
            )
        }
    }
}

// 分段按钮实现

data class RadioOption(val key: String, val label: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedButtonSetting(
    settingKey: Preferences.Key<String>,
    title: String,
    options: List<RadioOption>,
    defaultOptionKey: String,
    modifier: Modifier = Modifier,
    titleExtraContent: (@Composable () -> Unit)? = null,    //标题旁边的内容
) {
    var currentSelection by rememberDataStoreState(settingKey, defaultOptionKey)

    Column(
        modifier = modifier.padding(start = 8.dp, end = 8.dp),
        verticalArrangement = Arrangement.spacedBy((-12).dp)
    ) {
        // 字体和旁边的按钮设置
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 16.dp), // 调整内边距以适应IconButton
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start   // 从左到右排列
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            // 如果传入了额外内容，就在这里显示它
            titleExtraContent?.invoke()
        }

        SingleChoiceSegmentedButtonRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            options.forEachIndexed { index, option ->
                // ✨ 关键改动：根据位置动态计算形状！
                val shape = when (index) {
                    // 第一个按钮：左边是圆角，右边是直角
                    0 -> RoundedCornerShape(topStartPercent = 50, bottomStartPercent = 50)
                    // 最后一个按钮：左边是直角，右边是圆角
                    options.lastIndex -> RoundedCornerShape(
                        topEndPercent = 50,
                        bottomEndPercent = 50
                    )
                    // 中间的按钮：两边都是直角
                    else -> RectangleShape
                }

                SegmentedButton(
                    shape = shape, // ✨ 使用我们动态计算的形状
                    onClick = { currentSelection = option.key },
                    selected = currentSelection == option.key
                ) {
                    Text(option.label)
                }
            }
        }

    }
}

// 带tooltip的infoIcon实现
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoDialogIcon(
    title: String,
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.Info,
    contentDescription: String? = null,
) {
    // ✨ 关键：组件自己管理自己的弹窗状态，外部完全无需关心！
    var showDialog by remember { mutableStateOf(false) }

    // 1. 这是用户能看到的触发器：一个图标按钮
    IconButton(
        onClick = { showDialog = true }, // 点击时，只改变自己的内部状态
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription
        )
    }

    // 2. 这是与触发器绑定的弹窗UI
    //    当内部状态为 true 时，它就会自动显示出来
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text(text = title) },
            text = { Text(text = text) },
            confirmButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }
}

@Composable
fun SliderSettingItem(
    key: Preferences.Key<Long>,
    defaultValue: Long,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    valueRange: LongRange,
    step: Long, // 单步步长
    modifier: Modifier = Modifier,
) {
    // 使用 Hook 来自动同步 DataStore
    var currentValue by rememberDataStoreState(key, defaultValue)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {},
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 左侧的图标
            Box(modifier = Modifier.padding(end = 16.dp)) {
                icon()
            }
            // 右侧的文字和滑块
            Column(modifier = Modifier.weight(1f)) {
                // 标题和当前值
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    // 实时显示当前选中的值
                    Text(
                        text = "$currentValue ms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // 副标题
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // 滑块本体
                Slider(
                    value = currentValue.toFloat(),
                    onValueChange = {
                        // 当用户滑动时，更新状态
                        currentValue = it.roundToLong()
                    },
                    valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                    steps = ((valueRange.last - valueRange.first) / step - 1).toInt(), // 设置步数，让滑块可以吸附到整数值
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

// 新增一个可以点击的颜色设置，用来设置一个RGBA颜色
@Composable
fun ColorSettingItem(
    key: Preferences.Key<String>,
    defaultValue: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    // ✨ 核心修正 1：我们现在需要两个状态
    // `storedColorHex` 是我们与DataStore同步的“仓库”状态
    var storedColorHex by rememberDataStoreState(key, defaultValue)
    // `displayedColorHex` 是我们UI上立即显示的“公告板”状态
    var displayedColorHex by remember { mutableStateOf(defaultValue) }
    var showDialog by remember { mutableStateOf(false) }

    // ✨ 核心修正 2：用 LaunchedEffect 来保持“公告板”和“仓库”同步
    // 当 `storedColorHex` (仓库) 因任何原因改变时，立刻更新 `displayedColorHex` (公告板)
    LaunchedEffect(storedColorHex) {
        displayedColorHex = storedColorHex
    }

    // ✨ 核心修正 3：UI现在完全信任“公告板”上的颜色
    val currentColor = try {
        Color(displayedColorHex.toColorInt())
    } catch (e: Exception) {
        Color.Red
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Palette, contentDescription = "send btn overlay color")
        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = subtitle, style = MaterialTheme.typography.bodyMedium,
                color = LocalContentColor.current.copy(alpha = 0.6f)
            )
        }
        // 右侧的颜色预览
        Surface(
            modifier = Modifier.size(width = 50.dp, height = 30.dp),
            shape = RoundedCornerShape(8.dp), // 使用圆角矩形
            color = currentColor,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
        ) {}
    }

    // 当 showDialog 为 true 时，显示我们的颜色选择对话框
    if (showDialog) {
        ColorPickerDialog(
            initialColorHex = displayedColorHex,
            onDismissRequest = { showDialog = false },
            onColorSelected = { newColorHex ->
                // ✨ 核心修正 5：当用户选择新颜色时...
                // 1. 立刻更新“公告板”，UI瞬间响应！
                displayedColorHex = newColorHex
                // 2. 同时派出“慢性子信使”去更新“仓库”
                storedColorHex = newColorHex
                // 3. 关闭对话框
                showDialog = false
            }
        )
    }
}


/**
 * ✨ [新增] 我们的自定义颜色选择对话框。
 */
@Composable
private fun ColorPickerDialog(
    initialColorHex: String,
    onDismissRequest: () -> Unit,
    onColorSelected: (String) -> Unit,
) {
    // 对话框内部的临时状态，只有点“确认”时才会更新到外面
    var tempColorHex by remember { mutableStateOf(initialColorHex) }
    val isHexValid = remember(tempColorHex) {
        // 正则表达式，用于验证6位或8位Hex颜色代码（可带#号）
        tempColorHex.matches("^#?([0-9a-fA-F]{6}|[0-9a-fA-F]{8})$".toRegex())
    }
    val errorColor = MaterialTheme.colorScheme.error
    val parsedColor = remember(tempColorHex, isHexValid) {
        if (isHexValid) {
            try {
                Color(if (tempColorHex.startsWith("#")) tempColorHex.toColorInt() else "#$tempColorHex".toColorInt())
            } catch (e: Exception) {
                errorColor
            }
        } else {
            errorColor
        }
    }

    // 一些预设的颜色，方便用户快速选择
    val predefinedColors = listOf(
        "#80FF69B4", "#80FF4500", "#80FFD700", "#80ADFF2F",
        "#8000CED1", "#801E90FF", "#809370DB", "#80FFFFFF",
        "#80C0C0C0", "#FF808080", "#80000000", "#5066ccff",
        "#00000000" //纯透明
    )

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(stringResource(R.string.pick_color)) },
        text = {
            Column {
                // 颜色预览和Hex输入框
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(    //左侧的颜色预览
                        modifier = Modifier.size(40.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = parsedColor,
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        )
                    ) {}
                    Spacer(modifier = Modifier.width(16.dp))
                    TextField(
                        value = tempColorHex,
                        onValueChange = { tempColorHex = it },
                        label = { Text("Hex (A)RGB") },
                        isError = !isHexValid,
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                // 预设颜色网格
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(predefinedColors.size) { index ->
                        val colorHex = predefinedColors[index]
                        val color = Color(colorHex.toColorInt())
                        val isSelected = tempColorHex.equals(colorHex, ignoreCase = true)

                        Surface(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { tempColorHex = colorHex },
                            shape = RoundedCornerShape(8.dp),
                            color = color,
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                            ),
                        ) {
                            // Surface 的 content lambda 提供了一个干净的 BoxScope，消除了歧义
                            AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioLowBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                ) + fadeIn(animationSpec = tween(250)),
                                exit = scaleOut() + fadeOut()
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                                    modifier = Modifier.align(Alignment.CenterHorizontally) // 确保图标居中
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onColorSelected(tempColorHex) },
                enabled = isHexValid // 只有当输入的Hex有效时才能确认
            ) {
                Text(stringResource(R.string.accept))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}


/**
 * ✨ 全新：一个用于选择一个数值区间的设置项组件
 */
@Composable
fun RangeSliderSettingItem(
    minKey: Preferences.Key<Int>,
    maxKey: Preferences.Key<Int>,
    defaultMin: Int,
    defaultMax: Int,
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    valueRange: IntRange,
    step: Int,
    modifier: Modifier = Modifier,
) {
    // 使用 Hook 分别管理最小值和最大值的状态
    var currentMin by rememberDataStoreState(minKey, defaultMin)
    var currentMax by rememberDataStoreState(maxKey, defaultMax)

    // RangeSlider 需要一个 Range 类型的 state，我们在这里组合一下
    val currentRange by remember(currentMin, currentMax) {
        mutableStateOf(currentMin.toFloat()..currentMax.toFloat())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.padding(end = 16.dp)) { icon() }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    // 实时显示当前选中的范围
                    Text(
                        text = "$currentMin - $currentMax",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                RangeSlider(
                    value = currentRange,
                    onValueChange = { newRange ->
                        // 当用户滑动时，我们只更新本地的 state 以提供实时反馈
                        // 注意：这里我们不直接写入 DataStore，避免过于频繁的IO操作
                        currentMin = newRange.start.roundToInt()
                        currentMax = newRange.endInclusive.roundToInt()
                    },
                    // ✨ 当用户滑动结束后，才把最终确定的值写入 DataStore
                    onValueChangeFinished = {
                        // 因为我们的 by rememberDataStoreState 委托会自动保存，
                        // 所以这里实际上是触发了最终的赋值操作，从而写入
                    },
                    valueRange = valueRange.first.toFloat()..valueRange.last.toFloat(),
                    // 计算步数，(10-1)/1 = 9个档位，所以是8个间隔
                    steps = ((valueRange.last - valueRange.first) / step) - 1,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}