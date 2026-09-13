package app.libreshot.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Original line glyphs in the spirit of iOS markup icons: 24px grid, 1.75px stroke,
 * rounded caps. Not derived from SF Symbols.
 */
object AppIcons {

    private fun glyph(name: String, vararg paths: String): ImageVector =
        ImageVector.Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            paths.forEach { d ->
                addPath(
                    pathData = PathParser().parsePathString(d).toNodes(),
                    stroke = SolidColor(Color.White),
                    strokeLineWidth = 1.75f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                )
            }
        }.build()

    val Share by lazy {
        glyph(
            "Share",
            "M12 3.2v10.3",
            "M8.4 6.7 12 3.2l3.6 3.5",
            "M7.2 9.6H6.4a2.6 2.6 0 0 0-2.6 2.6v6.9a2.6 2.6 0 0 0 2.6 2.6h11.2a2.6 2.6 0 0 0 2.6-2.6v-6.9a2.6 2.6 0 0 0-2.6-2.6h-0.8",
        )
    }

    val Undo by lazy {
        glyph("Undo", "M8.6 13.2 4 8.6l4.6-4.6", "M4 8.6h9.8a5.9 5.9 0 0 1 0 11.8h-3.3")
    }

    val Redo by lazy {
        glyph("Redo", "M15.4 13.2 20 8.6l-4.6-4.6", "M20 8.6h-9.8a5.9 5.9 0 0 0 0 11.8h3.3")
    }

    val Pen by lazy {
        glyph(
            "Pen",
            "M4.6 19.4l0.8-3.1L16 5.7a2.06 2.06 0 0 1 2.9 0l0.4 0.4a2.06 2.06 0 0 1 0 2.9L8.7 19.6l-4.1-0.2z",
            "M14.6 7.1l3.3 3.3",
        )
    }

    val Marker by lazy {
        glyph(
            "Marker",
            "M4 20.2h5.6",
            "M6.4 16.6l7.9-7.9a1.77 1.77 0 0 1 2.5 0l0.6 0.6a1.77 1.77 0 0 1 0 2.5l-7.9 7.9-3.6 0.6 0.5-3.7z",
        )
    }

    val Pencil by lazy {
        glyph(
            "Pencil",
            "M4.3 19.7l0.6-2.8 9.7-9.7a1.7 1.7 0 0 1 2.4 0l0.7 0.7a1.7 1.7 0 0 1 0 2.4l-9.7 9.7-3.7-0.3z",
            "M13.8 8.4l2.8 2.8",
        )
    }

    val Eraser by lazy {
        glyph(
            "Eraser",
            "M7.1 20.4h10.4",
            "M4.9 14.1l7.4-7.4a1.7 1.7 0 0 1 2.4 0l3.4 3.4a1.7 1.7 0 0 1 0 2.4l-6.2 6.2H9.1l-4.2-4.2a1.7 1.7 0 0 1 0-2.4z",
        )
    }

    val Crop by lazy {
        glyph(
            "Crop",
            "M6.6 2.4v13.9a2 2 0 0 0 2 2h13",
            "M2.4 6.6h13.9a2 2 0 0 1 2 2v13",
        )
    }

    val Check by lazy { glyph("Check", "M4.6 12.6l4.9 4.9 9.9-10.9") }

    val Close by lazy { glyph("Close", "M6.2 6.2l11.6 11.6", "M17.8 6.2 6.2 17.8") }

    val Add by lazy {
        glyph(
            "Add",
            "M12 7.8v8.4",
            "M7.8 12h8.4",
            "M12 3.4a8.6 8.6 0 1 1 -0.01 0z",
        )
    }
}
