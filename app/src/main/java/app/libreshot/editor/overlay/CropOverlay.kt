package app.libreshot.editor.overlay

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import app.libreshot.editor.model.CropHandle
import app.libreshot.editor.model.CropRect
import kotlin.math.abs
import kotlin.math.hypot

/**
 * Where the crop rect lands on screen given the canvas' letterbox + zoom/pan transform.
 * Mirrors MarkupCanvas' math: canvas = origin + norm * bitmap * fitScale, then
 * screen = canvas * zoom + pan (transform origin is the canvas top-left).
 */
fun cropRectOnScreen(
    crop: CropRect,
    bitmapWidth: Int,
    bitmapHeight: Int,
    fitScale: Float,
    origin: Offset,
    zoom: Float,
    pan: Offset,
): Rect {
    fun x(norm: Float) = (origin.x + norm * bitmapWidth * fitScale) * zoom + pan.x
    fun y(norm: Float) = (origin.y + norm * bitmapHeight * fitScale) * zoom + pan.y
    return Rect(x(crop.left), y(crop.top), x(crop.right), y(crop.bottom))
}

/**
 * Which handle (if any) a screen-space touch lands on. Corners win over edges and use a
 * generous radius; edges use a thinner band centred on the crop border.
 */
fun hitTestCropHandle(
    pos: Offset,
    rect: Rect,
    cornerRadiusPx: Float,
    edgeBandPx: Float,
): CropHandle? {
    val corners = listOf(
        CropHandle.TOP_LEFT to rect.topLeft,
        CropHandle.TOP_RIGHT to rect.topRight,
        CropHandle.BOTTOM_LEFT to rect.bottomLeft,
        CropHandle.BOTTOM_RIGHT to rect.bottomRight,
    )
    corners.forEach { (handle, corner) ->
        if (hypot(pos.x - corner.x, pos.y - corner.y) <= cornerRadiusPx) return handle
    }
    val slack = cornerRadiusPx / 2f
    val withinX = pos.x in (rect.left - slack)..(rect.right + slack)
    val withinY = pos.y in (rect.top - slack)..(rect.bottom + slack)
    return when {
        abs(pos.x - rect.left) <= edgeBandPx && withinY -> CropHandle.LEFT
        abs(pos.x - rect.right) <= edgeBandPx && withinY -> CropHandle.RIGHT
        abs(pos.y - rect.top) <= edgeBandPx && withinX -> CropHandle.TOP
        abs(pos.y - rect.bottom) <= edgeBandPx && withinX -> CropHandle.BOTTOM
        else -> null
    }
}

/**
 * iOS-style crop chrome drawn above the markup canvas, in screen space (it does not zoom):
 * dimmed scrim over the excluded region, rule-of-thirds grid while a handle is dragged,
 * four L-shaped corner brackets (3 dp stroke, 24 dp arms) and subtle edge grabbers.
 */
@Composable
fun CropOverlay(rect: Rect, dimOutside: Boolean, showGrid: Boolean) {
    Canvas(Modifier.fillMaxSize()) {
        if (dimOutside) {
            val scrim = Color.Black.copy(alpha = 0.45f)
            drawRect(scrim, size = Size(size.width, rect.top.coerceAtLeast(0f)))
            drawRect(
                scrim,
                topLeft = Offset(0f, rect.bottom),
                size = Size(size.width, (size.height - rect.bottom).coerceAtLeast(0f)),
            )
            drawRect(
                scrim,
                topLeft = Offset(0f, rect.top),
                size = Size(rect.left.coerceAtLeast(0f), rect.height),
            )
            drawRect(
                scrim,
                topLeft = Offset(rect.right, rect.top),
                size = Size((size.width - rect.right).coerceAtLeast(0f), rect.height),
            )
        }
        if (showGrid) {
            val grid = Color.White.copy(alpha = 0.35f)
            val thin = 1.dp.toPx()
            for (i in 1..2) {
                val gx = rect.left + rect.width * i / 3f
                drawLine(grid, Offset(gx, rect.top), Offset(gx, rect.bottom), thin)
                val gy = rect.top + rect.height * i / 3f
                drawLine(grid, Offset(rect.left, gy), Offset(rect.right, gy), thin)
            }
        }

        val stroke = 3.dp.toPx()
        val arm = 24.dp.toPx()
        val white = Color.White
        fun bracket(corner: Offset, dx: Float, dy: Float) {
            drawLine(
                white,
                Offset(corner.x + dx * arm, corner.y),
                Offset(corner.x, corner.y),
                stroke,
                cap = StrokeCap.Round,
            )
            drawLine(
                white,
                Offset(corner.x, corner.y + dy * arm),
                Offset(corner.x, corner.y),
                stroke,
                cap = StrokeCap.Round,
            )
        }
        bracket(rect.topLeft, 1f, 1f)
        bracket(rect.topRight, -1f, 1f)
        bracket(rect.bottomLeft, 1f, -1f)
        bracket(rect.bottomRight, -1f, -1f)

        val grab = 16.dp.toPx()
        val grabStroke = 3.dp.toPx()
        val grabColor = Color.White.copy(alpha = 0.6f)
        drawLine(
            grabColor,
            Offset(rect.center.x - grab / 2f, rect.top),
            Offset(rect.center.x + grab / 2f, rect.top),
            grabStroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            grabColor,
            Offset(rect.center.x - grab / 2f, rect.bottom),
            Offset(rect.center.x + grab / 2f, rect.bottom),
            grabStroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            grabColor,
            Offset(rect.left, rect.center.y - grab / 2f),
            Offset(rect.left, rect.center.y + grab / 2f),
            grabStroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            grabColor,
            Offset(rect.right, rect.center.y - grab / 2f),
            Offset(rect.right, rect.center.y + grab / 2f),
            grabStroke,
            cap = StrokeCap.Round,
        )
    }
}
