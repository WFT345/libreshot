package app.libreshot.editor.overlay

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import app.libreshot.editor.model.CropHandle
import app.libreshot.editor.model.CropRect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CropOverlayTest {

    private val cornerRadius = 28f
    private val edgeBand = 14f

    @Test
    fun `screen rect maps normalized crop through fit, zoom and pan`() {
        val rect = cropRectOnScreen(
            crop = CropRect(0.1f, 0.2f, 0.9f, 0.8f),
            bitmapWidth = 1000,
            bitmapHeight = 2000,
            fitScale = 0.5f,
            origin = Offset(10f, 20f),
            zoom = 1f,
            pan = Offset.Zero,
        )
        assertEquals(Rect(60f, 220f, 460f, 820f), rect)
    }

    @Test
    fun `screen rect applies zoom and pan around the top-left origin`() {
        val rect = cropRectOnScreen(
            crop = CropRect(0.1f, 0.2f, 0.9f, 0.8f),
            bitmapWidth = 1000,
            bitmapHeight = 2000,
            fitScale = 0.5f,
            origin = Offset(10f, 20f),
            zoom = 2f,
            pan = Offset(5f, 7f),
        )
        assertEquals(Rect(125f, 447f, 925f, 1647f), rect)
    }

    @Test
    fun `hit test finds each corner within its radius`() {
        val rect = Rect(100f, 100f, 400f, 500f)
        assertEquals(
            CropHandle.TOP_LEFT,
            hitTestCropHandle(Offset(110f, 90f), rect, cornerRadius, edgeBand),
        )
        assertEquals(
            CropHandle.TOP_RIGHT,
            hitTestCropHandle(Offset(395f, 105f), rect, cornerRadius, edgeBand),
        )
        assertEquals(
            CropHandle.BOTTOM_LEFT,
            hitTestCropHandle(Offset(95f, 510f), rect, cornerRadius, edgeBand),
        )
        assertEquals(
            CropHandle.BOTTOM_RIGHT,
            hitTestCropHandle(Offset(410f, 490f), rect, cornerRadius, edgeBand),
        )
    }

    @Test
    fun `hit test finds edge bands away from corners`() {
        val rect = Rect(100f, 100f, 400f, 500f)
        assertEquals(
            CropHandle.LEFT,
            hitTestCropHandle(Offset(105f, 300f), rect, cornerRadius, edgeBand),
        )
        assertEquals(
            CropHandle.RIGHT,
            hitTestCropHandle(Offset(392f, 300f), rect, cornerRadius, edgeBand),
        )
        assertEquals(
            CropHandle.TOP,
            hitTestCropHandle(Offset(250f, 108f), rect, cornerRadius, edgeBand),
        )
        assertEquals(
            CropHandle.BOTTOM,
            hitTestCropHandle(Offset(250f, 495f), rect, cornerRadius, edgeBand),
        )
    }

    @Test
    fun `hit test prefers corners over edges`() {
        val rect = Rect(100f, 100f, 400f, 500f)
        assertEquals(
            CropHandle.TOP_LEFT,
            hitTestCropHandle(Offset(104f, 104f), rect, cornerRadius, edgeBand),
        )
    }

    @Test
    fun `hit test misses the middle and far outside`() {
        val rect = Rect(100f, 100f, 400f, 500f)
        assertNull(hitTestCropHandle(Offset(250f, 300f), rect, cornerRadius, edgeBand))
        assertNull(hitTestCropHandle(Offset(0f, 0f), rect, cornerRadius, edgeBand))
    }
}
