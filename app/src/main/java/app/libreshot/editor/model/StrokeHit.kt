package app.libreshot.editor.model

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/** Distance from [p] to the segment [a]-[b]. */
fun pointToSegment(p: Offset, a: Offset, b: Offset): Float {
    val abx = b.x - a.x
    val aby = b.y - a.y
    val lengthSq = abx * abx + aby * aby
    if (lengthSq == 0f) return hypot(p.x - a.x, p.y - a.y)
    val t = (((p.x - a.x) * abx + (p.y - a.y) * aby) / lengthSq).coerceIn(0f, 1f)
    return hypot(p.x - (a.x + t * abx), p.y - (a.y + t * aby))
}

/** Nearest stroke to [p] within [tolerancePx] of its stroke edge, or null. Topmost stroke wins. */
fun hitTestStroke(strokes: List<Stroke>, p: Offset, tolerancePx: Float): Stroke? =
    strokes.asReversed().firstOrNull { stroke ->
        val reach = stroke.widthPx / 2f + tolerancePx
        stroke.points.zipWithNext().any { (a, b) -> pointToSegment(p, a, b) <= reach }
            || (stroke.points.size == 1 && hypot(p.x - stroke.points[0].x, p.y - stroke.points[0].y) <= reach)
    }
