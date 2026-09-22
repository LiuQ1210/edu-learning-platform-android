package com.github.learningplatform.ui.checkin

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntSize

/**
 * 九宫格手势绘制组件（手势签到）。
 *
 * 索引按行优先排列，与 sign_task.gesture_pattern 的存储格式一致：
 * ```
 * 0 1 2
 * 3 4 5
 * 6 7 8
 * ```
 * 回调返回选中序列，可用 [patternToString] 转成 "0,1,2,5,8" 提交给后端。
 */
@Composable
fun GestureLockView(
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    /** 变化时清空已绘制内容，用于「重绘」 */
    resetKey: Int = 0,
    onPatternChanged: (List<Int>) -> Unit = {}
) {
    var selected by remember(resetKey) { mutableStateOf<List<Int>>(emptyList()) }
    var dragPosition by remember { mutableStateOf<Offset?>(null) }

    val activeColor = MaterialTheme.colorScheme.primary
    val idleColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(enabled) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = { start ->
                        selected = listOfNotNull(hitTest(start, size))
                        dragPosition = start
                    },
                    onDrag = { change, _ ->
                        dragPosition = change.position
                        val hit = hitTest(change.position, size)
                        if (hit != null && hit !in selected) {
                            selected = selected + hit
                        }
                    },
                    onDragEnd = {
                        dragPosition = null
                        onPatternChanged(selected)
                    },
                    onDragCancel = {
                        dragPosition = null
                        onPatternChanged(selected)
                    }
                )
            }
    ) {
        val cell = size.width / 3f
        val dotRadius = cell * 0.11f
        val lineWidth = dotRadius * 0.62f

        fun center(index: Int) = Offset(
            cell * (index % 3 + 0.5f),
            cell * (index / 3 + 0.5f)
        )

        // 已连接的线段
        for (i in 0 until selected.size - 1) {
            drawLine(
                color = activeColor,
                start = center(selected[i]),
                end = center(selected[i + 1]),
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )
        }

        // 手指当前位置到最后一个点的牵引线
        val drag = dragPosition
        if (drag != null && selected.isNotEmpty()) {
            drawLine(
                color = activeColor.copy(alpha = 0.5f),
                start = center(selected.last()),
                end = drag,
                strokeWidth = lineWidth,
                cap = StrokeCap.Round
            )
        }

        // 九个圆点
        for (i in 0..8) {
            val c = center(i)
            if (i in selected) {
                drawCircle(color = activeColor.copy(alpha = 0.25f), radius = dotRadius * 2.1f, center = c)
                drawCircle(color = activeColor, radius = dotRadius, center = c)
            } else {
                drawCircle(color = idleColor, radius = dotRadius, center = c)
                drawCircle(color = Color.White, radius = dotRadius * 0.45f, center = c)
            }
        }
    }
}

/** 命中检测：手指落在某个点附近则返回其索引 */
private fun hitTest(position: Offset, size: IntSize): Int? {
    val cell = size.width / 3f
    if (cell <= 0f) return null
    val threshold = cell * 0.32f
    for (i in 0..8) {
        val c = Offset(cell * (i % 3 + 0.5f), cell * (i / 3 + 0.5f))
        if ((position - c).getDistance() <= threshold) return i
    }
    return null
}

/** 转成后端约定格式，如 [0,1,2,5,8] -> "0,1,2,5,8" */
fun patternToString(pattern: List<Int>): String = pattern.joinToString(",")

/** 解析后端预设图案，用于展示 */
fun stringToPattern(value: String?): List<Int> =
    value?.split(',')?.mapNotNull { it.trim().toIntOrNull() } ?: emptyList()