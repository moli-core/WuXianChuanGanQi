package com.smarthome.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smarthome.app.ui.theme.DoubaoOrange
import com.smarthome.app.ui.theme.DoubaoOrangeLight
import com.smarthome.app.ui.theme.DoubaoPeach

/**
 * 豆包卡通形象 - 用 Canvas 绘制的可爱圆脸
 */
@Composable
fun DoubaoCharacter(
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    expression: Expression = Expression.HAPPY
) {
    Canvas(modifier = modifier.size(size)) {
        val cx = size.toPx() / 2
        val cy = size.toPx() / 2
        val radius = size.toPx() / 2 - 4

        // 身体/脸 - 暖橙色圆
        drawCircle(
            color = DoubaoOrange,
            radius = radius,
            center = Offset(cx, cy),
            style = Fill
        )

        // 高光
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = radius * 0.35f,
            center = Offset(cx - radius * 0.25f, cy - radius * 0.25f)
        )

        // 眼睛
        val eyeY = cy - radius * 0.1f
        val eyeSpacing = radius * 0.25f
        val eyeR = radius * 0.08f
        drawCircle(Color(0xFF2D2D2D), eyeR, Offset(cx - eyeSpacing, eyeY))
        drawCircle(Color(0xFF2D2D2D), eyeR, Offset(cx + eyeSpacing, eyeY))

        // 眼睛高光
        drawCircle(Color.White, eyeR * 0.4f, Offset(cx - eyeSpacing + eyeR * 0.3f, eyeY - eyeR * 0.3f))
        drawCircle(Color.White, eyeR * 0.4f, Offset(cx + eyeSpacing + eyeR * 0.3f, eyeY - eyeR * 0.3f))

        // 腮红
        drawCircle(DoubaoPeach, radius * 0.12f, Offset(cx - radius * 0.4f, cy + radius * 0.15f))
        drawCircle(DoubaoPeach, radius * 0.12f, Offset(cx + radius * 0.4f, cy + radius * 0.15f))

        // 嘴巴（根据表情）
        when (expression) {
            Expression.HAPPY -> {
                drawArc(
                    color = Color(0xFF2D2D2D),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - radius * 0.15f, cy + radius * 0.05f),
                    size = Size(radius * 0.3f, radius * 0.2f),
                    style = Stroke(width = 2f)
                )
            }
            Expression.SMILE -> {
                drawLine(
                    Color(0xFF2D2D2D),
                    Offset(cx - radius * 0.12f, cy + radius * 0.18f),
                    Offset(cx + radius * 0.12f, cy + radius * 0.18f),
                    strokeWidth = 2f
                )
            }
            Expression.WINK -> {
                drawCircle(Color(0xFF2D2D2D), eyeR, Offset(cx - eyeSpacing, eyeY))
                // 闭眼（左眼） - 画一条线
                drawLine(Color(0xFF2D2D2D),
                    Offset(cx + eyeSpacing - eyeR, eyeY),
                    Offset(cx + eyeSpacing + eyeR, eyeY),
                    strokeWidth = 2f
                )
                drawArc(
                    color = Color(0xFF2D2D2D),
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(cx - radius * 0.12f, cy + radius * 0.05f),
                    size = Size(radius * 0.24f, radius * 0.18f),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

enum class Expression { HAPPY, SMILE, WINK }
