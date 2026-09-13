package app.libreshot.editor.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class StrokeHitTest {

    private fun line(a: Offset, b: Offset, width: Float = 4f) = Stroke(
        points = listOf(a, b),
        color = Color.Black,
        widthPx = width,
        alpha = 1f,
        tool = Tool.PEN,
    )

    @Test
    fun `point near segment hits`() {
        val stroke = line(Offset(0f, 0f), Offset(100f, 0f))
        assertSame(stroke, hitTestStroke(listOf(stroke), Offset(50f, 3f), tolerancePx = 4f))
    }

    @Test
    fun `point far away misses`() {
        val stroke = line(Offset(0f, 0f), Offset(100f, 0f))
        assertNull(hitTestStroke(listOf(stroke), Offset(50f, 40f), tolerancePx = 4f))
    }

    @Test
    fun `topmost stroke wins`() {
        val bottom = line(Offset(0f, 0f), Offset(100f, 0f))
        val top = line(Offset(0f, 2f), Offset(100f, 2f))
        assertSame(top, hitTestStroke(listOf(bottom, top), Offset(50f, 1f), tolerancePx = 4f))
    }

    @Test
    fun `distance to segment endpoints is clamped`() {
        val distance = pointToSegment(Offset(-10f, 0f), Offset(0f, 0f), Offset(100f, 0f))
        assertEquals(10f, distance, 0.001f)
    }
}
