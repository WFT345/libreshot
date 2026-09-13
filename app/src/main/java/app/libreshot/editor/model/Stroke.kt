package app.libreshot.editor.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class Tool { PEN, MARKER, PENCIL, ERASER }

/** A single markup stroke. Points and width are in bitmap pixels, not screen pixels. */
data class Stroke(
    val points: List<Offset>,
    val color: Color,
    val widthPx: Float,
    val alpha: Float,
    val tool: Tool,
)
