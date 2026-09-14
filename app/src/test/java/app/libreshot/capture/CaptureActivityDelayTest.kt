package app.libreshot.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureActivityDelayTest {

    @Test
    fun `tile delay passes through unchanged`() {
        assertEquals(400L, CaptureActivity.clampDelay(400L))
    }

    @Test
    fun `negative delay clamps to zero`() {
        assertEquals(0L, CaptureActivity.clampDelay(-1L))
        assertEquals(0L, CaptureActivity.clampDelay(Long.MIN_VALUE))
    }

    @Test
    fun `oversized delay clamps to the maximum`() {
        assertEquals(
            CaptureActivity.MAX_CAPTURE_DELAY_MS,
            CaptureActivity.clampDelay(Long.MAX_VALUE),
        )
        assertEquals(
            CaptureActivity.MAX_CAPTURE_DELAY_MS,
            CaptureActivity.clampDelay(60_000L),
        )
    }

    @Test
    fun `maximum is above the tile delay and still short`() {
        assertEquals(2_000L, CaptureActivity.MAX_CAPTURE_DELAY_MS)
    }
}
