package app.libreshot.export

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import androidx.compose.ui.graphics.toArgb
import app.libreshot.editor.model.CropRect
import app.libreshot.editor.model.Stroke
import app.libreshot.editor.model.Tool

/** Renders the final composite: original capture plus markup, then crop, in bitmap space. */
object Compositor {

    fun render(original: Bitmap, strokes: List<Stroke>, crop: CropRect = CropRect.FULL): Bitmap {
        val out = original.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(out)
        strokes.forEach { drawStroke(canvas, it) }
        val px = crop.toPixelRect(original.width, original.height)
        if (px.isFull(original.width, original.height)) return out
        return Bitmap.createBitmap(out, px.left, px.top, px.width, px.height)
    }

    private fun drawStroke(canvas: Canvas, stroke: Stroke) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = stroke.color.copy(alpha = stroke.alpha.coerceIn(0f, 1f)).toArgb()
            strokeWidth = stroke.widthPx
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            if (stroke.tool == Tool.MARKER) blendMode = BlendMode.MULTIPLY
        }
        val points = stroke.points
        if (points.size == 1) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(points[0].x, points[0].y, stroke.widthPx / 2f, paint)
            return
        }
        paint.style = Paint.Style.STROKE
        val path = Path()
        path.moveTo(points[0].x, points[0].y)
        for (i in 1 until points.size - 1) {
            val cur = points[i]
            val next = points[i + 1]
            path.quadTo(cur.x, cur.y, (cur.x + next.x) / 2f, (cur.y + next.y) / 2f)
        }
        path.lineTo(points.last().x, points.last().y)
        canvas.drawPath(path, paint)
    }
}
