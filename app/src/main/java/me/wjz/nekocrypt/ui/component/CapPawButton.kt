package me.wjz.nekocrypt.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp

private object CatPawDefaults {
    const val RING_STROKE_WIDTH = 14f   // 外围圆弧段落粗度
    const val PAW_STROKE_WIDTH = 15f
    const val DESIGN_BASIS_DP = 290f

    // --- 尺寸比例 ---
    const val RING_ENABLED_RATIO = 1f // 激活时外圈尺寸比例 (等于基准大小)
    const val RING_DISABLED_RATIO = 270f / DESIGN_BASIS_DP // 未激活时外圈的尺寸比例
    const val CENTER_BUTTON_RATIO = 260f / DESIGN_BASIS_DP
    const val PAW_CANVAS_RATIO = 110f / DESIGN_BASIS_DP

    // --- 字体大小比例 ---
    const val FONT_SIZE_RATIO = 20f / DESIGN_BASIS_DP
    const val MIN_FONT_SIZE_SP = 12f

    // --- 猫爪内部绘制比例 (相对于猫爪Canvas) ---
    const val PALM_WIDTH_RATIO = 0.6f
    const val PALM_HEIGHT_RATIO = 0.45f
    const val PALM_Y_OFFSET_RATIO = 0.2f
    const val TOE_RADIUS_RATIO = 0.1f

    // --- 猫爪脚趾基础位置比例 (相对于猫爪Canvas) ---
    const val OUTER_TOE_X_RATIO = 0.35f
    const val OUTER_TOE_Y_RATIO = 0.08f
    const val INNER_TOE_X_RATIO = 0.15f
    const val INNER_TOE_Y_RATIO = 0.25f

    // --- 猫爪脚趾激活状态位移 (基于原始设计尺寸) ---
    const val PALM_Y_SHIFT = -10f
    const val OUTER_LEFT_TOE_X_SHIFT = -18f
    const val OUTER_LEFT_TOE_Y_SHIFT = -15f
    const val INNER_LEFT_TOE_X_SHIFT = -10f
    const val INNER_LEFT_TOE_Y_SHIFT = -25f
    const val INNER_RIGHT_TOE_X_SHIFT = 10f
    const val INNER_RIGHT_TOE_Y_SHIFT = -25f
    const val OUTER_RIGHT_TOE_X_SHIFT = 18f
    const val OUTER_RIGHT_TOE_Y_SHIFT = -15f
}

/**
 * ✨ 响应式猫爪按钮
 * 它会根据父组件提供的空间，自动调整自身大小和内部所有元素的比例。
 */
@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun CatPawButton(
    isEnabled: Boolean,
    statusText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val baseSize = min(maxWidth, maxHeight)
        val scaleFactor = baseSize.value / CatPawDefaults.DESIGN_BASIS_DP

        // --- 动画状态 ---
        val ringSize by animateDpAsState(
            targetValue = if (isEnabled) baseSize * CatPawDefaults.RING_ENABLED_RATIO else baseSize * CatPawDefaults.RING_DISABLED_RATIO,
            animationSpec = tween(600),
            label = "RingSizeAnimation"
        )
        val buttonFillColor by animateColorAsState(
            targetValue = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            animationSpec = tween(500),
            label = "ButtonFillAnimation"
        )
        val contentColor by animateColorAsState(
            targetValue = if (isEnabled) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(500),
            label = "ContentColorAnimation"
        )
        val shadowElevation by animateFloatAsState(
            targetValue = if (isEnabled) (baseSize.value * 0.055f) else (baseSize.value * 0.027f),
            animationSpec = tween(500),
            label = "ShadowElevation"
        )
        val rotationSpeed by animateFloatAsState(
            targetValue = if (isEnabled) 15f else 5f,
            animationSpec = tween(1500),
            label = "RotationSpeedAnimation"
        )
        var rotationAngle by remember { mutableFloatStateOf(0f) }
        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        val arcColor1 by animateColorAsState(
            targetValue = if (isEnabled) MaterialTheme.colorScheme.primary else outlineColor,
            animationSpec = tween(700),
            label = "ArcColor1"
        )
        val arcColor2 by animateColorAsState(
            targetValue = if (isEnabled) MaterialTheme.colorScheme.tertiary else outlineColor,
            animationSpec = tween(700),
            label = "ArcColor2"
        )
        val arcBrush = Brush.sweepGradient(colors = listOf(arcColor1, arcColor2, arcColor1))

        val palmOffsetY by animateFloatAsState(if (isEnabled) CatPawDefaults.PALM_Y_SHIFT * scaleFactor else 0f, tween(400), label = "PalmOffsetY")
        val outerLeftToeX by animateFloatAsState(if (isEnabled) CatPawDefaults.OUTER_LEFT_TOE_X_SHIFT * scaleFactor else 0f, tween(400), label = "OuterLeftToeX")
        val outerLeftToeY by animateFloatAsState(if (isEnabled) CatPawDefaults.OUTER_LEFT_TOE_Y_SHIFT * scaleFactor else 0f, tween(400), label = "OuterLeftToeY")
        val innerLeftToeX by animateFloatAsState(if (isEnabled) CatPawDefaults.INNER_LEFT_TOE_X_SHIFT * scaleFactor else 0f, tween(400), label = "InnerLeftToeX")
        val innerLeftToeY by animateFloatAsState(if (isEnabled) CatPawDefaults.INNER_LEFT_TOE_Y_SHIFT * scaleFactor else 0f, tween(400), label = "InnerLeftToeY")
        val innerRightToeX by animateFloatAsState(if (isEnabled) CatPawDefaults.INNER_RIGHT_TOE_X_SHIFT * scaleFactor else 0f, tween(400), label = "InnerRightToeX")
        val innerRightToeY by animateFloatAsState(if (isEnabled) CatPawDefaults.INNER_RIGHT_TOE_Y_SHIFT * scaleFactor else 0f, tween(400), label = "InnerRightToeY")
        val outerRightToeX by animateFloatAsState(if (isEnabled) CatPawDefaults.OUTER_RIGHT_TOE_X_SHIFT * scaleFactor else 0f, tween(400), label = "OuterRightToeX")
        val outerRightToeY by animateFloatAsState(if (isEnabled) CatPawDefaults.OUTER_RIGHT_TOE_Y_SHIFT * scaleFactor else 0f, tween(400), label = "OuterRightToeY")
        val gapAngle by animateFloatAsState(
            targetValue = if (isEnabled) 8f else 12f,
            animationSpec = tween(700),
            label = "GapAngleAnimation"
        )

        LaunchedEffect(Unit) {
            var lastFrameTimeNanos = 0L
            while (true) {
                withFrameNanos { frameTimeNanos ->
                    if (lastFrameTimeNanos != 0L) {
                        val deltaTimeMillis = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000f
                        val deltaAngle = (rotationSpeed * deltaTimeMillis) / 1000f
                        rotationAngle = (rotationAngle + deltaAngle) % 360f
                    }
                    lastFrameTimeNanos = frameTimeNanos
                }
            }
        }

        // --- 绘制部分 ---
        Canvas(modifier = Modifier.size(ringSize)) {
            val strokeWidth = CatPawDefaults.RING_STROKE_WIDTH
            val dashCount = 12
            val totalAnglePerDash = 360f / dashCount
            val dashAngle = totalAnglePerDash - gapAngle
            rotate(degrees = rotationAngle) {
                for (i in 0 until dashCount) {
                    drawArc(
                        brush = arcBrush,
                        startAngle = i * totalAnglePerDash,
                        sweepAngle = dashAngle,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }
            }
        }

        Surface(
            modifier = Modifier
                .size(baseSize * CatPawDefaults.CENTER_BUTTON_RATIO)
                .shadow(elevation = shadowElevation.dp, shape = CircleShape)
                .clip(CircleShape)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            color = buttonFillColor
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Canvas(modifier = Modifier.size(baseSize * CatPawDefaults.PAW_CANVAS_RATIO)) {
                    val strokeWidth = CatPawDefaults.PAW_STROKE_WIDTH

                    val palmSize = Size(size.width * CatPawDefaults.PALM_WIDTH_RATIO, size.height * CatPawDefaults.PALM_HEIGHT_RATIO)
                    val palmBaseCenter = Offset(center.x, center.y + size.height * CatPawDefaults.PALM_Y_OFFSET_RATIO)
                    val palmAnimatedCenter = palmBaseCenter.copy(y = palmBaseCenter.y + palmOffsetY)
                    val palmTopLeft = Offset(palmAnimatedCenter.x - palmSize.width / 2f, palmAnimatedCenter.y - palmSize.height / 2f)
                    drawOval(
                        color = contentColor,
                        topLeft = palmTopLeft,
                        size = palmSize,
                        style = Stroke(width = strokeWidth)
                    )

                    val toeRadius = size.width * CatPawDefaults.TOE_RADIUS_RATIO
                    val outerLeftBaseCenter = Offset(center.x - size.width * CatPawDefaults.OUTER_TOE_X_RATIO, center.y - size.height * CatPawDefaults.OUTER_TOE_Y_RATIO)
                    val innerLeftBaseCenter = Offset(center.x - size.width * CatPawDefaults.INNER_TOE_X_RATIO, center.y - size.height * CatPawDefaults.INNER_TOE_Y_RATIO)
                    val innerRightBaseCenter = Offset(center.x + size.width * CatPawDefaults.INNER_TOE_X_RATIO, center.y - size.height * CatPawDefaults.INNER_TOE_Y_RATIO)
                    val outerRightBaseCenter = Offset(center.x + size.width * CatPawDefaults.OUTER_TOE_X_RATIO, center.y - size.height * CatPawDefaults.OUTER_TOE_Y_RATIO)

                    drawCircle(
                        color = contentColor,
                        center = outerLeftBaseCenter.copy(x = outerLeftBaseCenter.x + outerLeftToeX, y = outerLeftBaseCenter.y + outerLeftToeY),
                        radius = toeRadius,
                        style = Stroke(width = strokeWidth)
                    )
                    drawCircle(
                        color = contentColor,
                        center = innerLeftBaseCenter.copy(x = innerLeftBaseCenter.x + innerLeftToeX, y = innerLeftBaseCenter.y + innerLeftToeY),
                        radius = toeRadius,
                        style = Stroke(width = strokeWidth)
                    )
                    drawCircle(
                        color = contentColor,
                        center = innerRightBaseCenter.copy(x = innerRightBaseCenter.x + innerRightToeX, y = innerRightBaseCenter.y + innerRightToeY),
                        radius = toeRadius,
                        style = Stroke(width = strokeWidth)
                    )
                    drawCircle(
                        color = contentColor,
                        center = outerRightBaseCenter.copy(x = outerRightBaseCenter.x + outerRightToeX, y = outerRightBaseCenter.y + outerRightToeY),
                        radius = toeRadius,
                        style = Stroke(width = strokeWidth)
                    )
                }

                AnimatedContent(
                    targetState = statusText,
                    transitionSpec = {
                        (slideInVertically { h -> h } + fadeIn(tween(250)))
                            .togetherWith(slideOutVertically { h -> -h } + fadeOut(tween(250)))
                            .using(SizeTransform(clip = false))
                    },
                    label = "StatusTextAnimation"
                ) { text ->
                    Text(
                        text = text,
                        color = contentColor,
                        fontSize = (baseSize.value * CatPawDefaults.FONT_SIZE_RATIO).coerceAtLeast(CatPawDefaults.MIN_FONT_SIZE_SP).sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}
