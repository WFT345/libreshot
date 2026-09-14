package app.libreshot.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackTapDetectorTest {

    private val g = 9.81f

    /** Flat gravity samples every 5 ms so the high-pass settles. Returns the next timestamp. */
    private fun warmUp(detector: BackTapDetector, fromMs: Long = 0L, count: Int = 100): Long {
        var t = fromMs
        repeat(count) {
            assertFalse(detector.onSample(t, 0f, 0f, g))
            t += 5
        }
        return t
    }

    /**
     * One physical tap: a 15 ms z transient (three super-threshold samples) followed by
     * six settle samples so the gravity estimate recovers. Returns true if any sample fired.
     */
    private fun tap(detector: BackTapDetector, atMs: Long, amplitude: Float = 4f): Boolean {
        var fired = detector.onSample(atMs, 0f, 0f, g + amplitude)
        fired = detector.onSample(atMs + 5, 0f, 0f, g + amplitude) || fired
        fired = detector.onSample(atMs + 10, 0f, 0f, g + amplitude * 0.6f) || fired
        for (i in 1..6) {
            fired = detector.onSample(atMs + 10 + i * 5L, 0f, 0f, g) || fired
        }
        return fired
    }

    @Test
    fun `clean pair fires on the second tap`() {
        val d = BackTapDetector(threshold = 2.5f)
        val t = warmUp(d)
        assertFalse(tap(d, t))
        assertTrue(tap(d, t + 200))
    }

    @Test
    fun `single tap does not fire`() {
        val d = BackTapDetector(threshold = 2.5f)
        val t = warmUp(d)
        assertFalse(tap(d, t))
    }

    @Test
    fun `pair slower than the window does not fire but re-arms`() {
        val d = BackTapDetector(threshold = 2.5f)
        val t = warmUp(d)
        assertFalse(tap(d, t))
        assertFalse(tap(d, t + 600))
        assertTrue(tap(d, t + 800))
    }

    @Test
    fun `taps closer than the minimum gap disarm`() {
        // 75 ms gap: outside the 60 ms spike-grouping window, inside the 80 ms minimum.
        val d = BackTapDetector(threshold = 2.5f)
        val t = warmUp(d)
        assertFalse(d.onSample(t, 0f, 0f, g + 4f))
        assertFalse(d.onSample(t + 75, 0f, 0f, g + 4f))
        // Disarmed: the next tap only arms again…
        assertFalse(tap(d, t + 400))
        // …and the one after that completes a pair.
        assertTrue(tap(d, t + 600))
    }

    @Test
    fun `sustained vibration never fires`() {
        val d = BackTapDetector(threshold = 2.5f)
        var t = warmUp(d)
        repeat(400) { // 2 s of every-sample super-threshold buzz
            assertFalse(d.onSample(t, 0f, 0f, g + 4f))
            t += 5
        }
    }

    @Test
    fun `sideways bump does not arm`() {
        val d = BackTapDetector(threshold = 2.5f)
        val t = warmUp(d)
        // x-dominant spike (side knock): must not count as a tap…
        assertFalse(d.onSample(t, 6f, 0f, g + 1f))
        // …so a single z tap 200 ms later has nothing to pair with.
        assertFalse(tap(d, t + 200))
    }

    @Test
    fun `cooldown suppresses an immediate second pair`() {
        val d = BackTapDetector(threshold = 2.5f)
        val t = warmUp(d)
        tap(d, t)
        assertTrue(tap(d, t + 200))
        assertFalse(tap(d, t + 500))
        assertFalse(tap(d, t + 700))
        // After the 1 s cooldown a fresh pair works again.
        assertFalse(tap(d, t + 1400))
        assertTrue(tap(d, t + 1600))
    }

    @Test
    fun `default threshold ignores incidental knocks and needs a firm tap`() {
        // Measured on a Pixel 10 Pro XL: idle handling peaks near 3 m/s², taps read 6-14.
        val soft = BackTapDetector()
        val tSoft = warmUp(soft)
        assertFalse(tap(soft, tSoft, amplitude = 4f))
        assertFalse(tap(soft, tSoft + 200, amplitude = 4f))

        val firm = BackTapDetector()
        val tFirm = warmUp(firm)
        assertFalse(tap(firm, tFirm, amplitude = 14f))
        assertTrue(tap(firm, tFirm + 200, amplitude = 14f))
    }

    @Test
    fun `sub-threshold wiggle never fires`() {
        val d = BackTapDetector(threshold = 2.5f)
        var t = warmUp(d)
        repeat(100) {
            assertFalse(d.onSample(t, 0f, 0f, g + 1.5f))
            t += 5
        }
    }
}
