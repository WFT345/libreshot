package app.libreshot.editor.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CropRectTest {

    @Test
    fun `full rect reports isFull, cropped does not`() {
        assertTrue(CropRect.FULL.isFull)
        assertFalse(CropRect(0.1f, 0f, 1f, 1f).isFull)
    }

    @Test
    fun `dragging top-left corner moves only left and top`() {
        val moved = CropRect.FULL.dragged(CropHandle.TOP_LEFT, 0.25f, 0.4f)
        assertEquals(CropRect(0.25f, 0.4f, 1f, 1f), moved)
    }

    @Test
    fun `dragging bottom-right corner moves only right and bottom`() {
        val moved = CropRect.FULL.dragged(CropHandle.BOTTOM_RIGHT, 0.6f, 0.7f)
        assertEquals(CropRect(0f, 0f, 0.6f, 0.7f), moved)
    }

    @Test
    fun `edge handles move one axis only`() {
        val start = CropRect(0.2f, 0.2f, 0.8f, 0.8f)
        assertEquals(0.1f, start.dragged(CropHandle.LEFT, 0.1f, 0.5f).left)
        assertEquals(0.2f, start.dragged(CropHandle.LEFT, 0.1f, 0.5f).top)
        assertEquals(0.9f, start.dragged(CropHandle.RIGHT, 0.9f, 0.0f).right)
        assertEquals(0.8f, start.dragged(CropHandle.RIGHT, 0.9f, 0.0f).bottom)
        assertEquals(0.05f, start.dragged(CropHandle.TOP, 0.5f, 0.05f).top)
        assertEquals(0.95f, start.dragged(CropHandle.BOTTOM, 0.5f, 0.95f).bottom)
    }

    @Test
    fun `handles never cross the opposite edge - min size is kept`() {
        val start = CropRect(0.4f, 0.4f, 0.6f, 0.6f)
        val squeezed = start.dragged(CropHandle.LEFT, 0.99f, 0.5f)
        assertEquals(start.right - CropRect.MIN_SIZE, squeezed.left)
        assertEquals(CropRect.MIN_SIZE, squeezed.width, 1e-6f)
        val squeezedUp = start.dragged(CropHandle.BOTTOM, 0.5f, 0.0f)
        assertEquals(CropRect.MIN_SIZE, squeezedUp.height, 1e-6f)
    }

    @Test
    fun `drag targets clamp into the bitmap`() {
        val moved = CropRect.FULL.dragged(CropHandle.TOP_LEFT, -0.5f, 1.7f)
        assertEquals(0f, moved.left)
        assertEquals(1f - CropRect.MIN_SIZE, moved.top)
    }

    @Test
    fun `toPixelRect maps fractions to pixels with at least 1 px`() {
        val px = CropRect(0.1f, 0.2f, 0.9f, 0.8f).toPixelRect(1080, 2340)
        assertEquals(108, px.left)
        assertEquals(468, px.top)
        assertEquals(972, px.right)
        assertEquals(1872, px.bottom)
        assertEquals(864, px.width)
        assertEquals(1404, px.height)
    }

    @Test
    fun `toPixelRect of FULL spans the bitmap and reports full`() {
        val px = CropRect.FULL.toPixelRect(1080, 2340)
        assertTrue(px.isFull(1080, 2340))
        assertEquals(1080, px.width)
    }

    @Test
    fun `toPixelRect of a tiny crop still yields 1 px`() {
        val px = CropRect(0f, 0f, 0.0001f, 0.0001f).toPixelRect(1080, 2340)
        assertTrue(px.width >= 1)
        assertTrue(px.height >= 1)
    }
}
