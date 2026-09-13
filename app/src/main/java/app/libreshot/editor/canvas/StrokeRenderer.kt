package app.libreshot.editor.canvas

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import app.libreshot.editor.model.Tool
import app.libreshot.editor.model.Stroke as MarkupStroke

/**
 * Draws a markup stroke into the fitted image rect: [origin] is the image's top-left in
 * canvas space, [fitScale] maps bitmap pixels to canvas pixels.
 */
fun DrawScope.drawStroke(stroke: MarkupStroke, fitScale: Float, origin: Offset) {
    val points = stroke.points
    val color = stroke.color.copy(alpha = stroke.alpha)
    val blend = if (stroke.tool == Tool.MARKER) BlendMode.Multiply else BlendMode.SrcOver
    val width = stroke.widthPx * fitScale
    if (points.size == 1) {
        drawCircle(
            color = color,
            radius = width / 2f,
            center = origin + points[0] * fitScale,
            blendMode = blend,
        )
        return
    }
    val path = Path()
    val first = origin + points[0] * fitScale
    path.moveTo(first.x, first.y)
    for (i in 1 until points.size - 1) {
        val cur = origin + points[i] * fitScale
        val next = origin + points[i + 1] * fitScale
        path.quadraticTo(cur.x, cur.y, (cur.x + next.x) / 2f, (cur.y + next.y) / 2f)
    }
    val last = origin + points.last() * fitScale
    path.lineTo(last.x, last.y)
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        blendMode = blend,
    )
}
