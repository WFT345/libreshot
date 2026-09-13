package app.libreshot.capture

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyChordDetectorTest {

    private val volDown = KeyEvent.KEYCODE_VOLUME_DOWN

    @Test
    fun `double press within window matches`() {
        val detector = KeyChordDetector(windowMs = 350)
        assertFalse(detector.onKeyDown(volDown, 1000))
        assertTrue(detector.onKeyDown(volDown, 1300))
    }

    @Test
    fun `press outside window does not match`() {
        val detector = KeyChordDetector(windowMs = 350)
        detector.onKeyDown(volDown, 1000)
        assertFalse(detector.onKeyDown(volDown, 1400))
    }

    @Test
    fun `triple press matches once then rearms`() {
        val detector = KeyChordDetector(windowMs = 350)
        assertFalse(detector.onKeyDown(volDown, 1000))
        assertTrue(detector.onKeyDown(volDown, 1200))
        assertFalse(detector.onKeyDown(volDown, 1400))
        assertTrue(detector.onKeyDown(volDown, 1600))
    }

    @Test
    fun `other key resets the chord`() {
        val detector = KeyChordDetector(windowMs = 350)
        detector.onKeyDown(volDown, 1000)
        assertFalse(detector.onKeyDown(KeyEvent.KEYCODE_VOLUME_UP, 1100))
        assertFalse(detector.onKeyDown(volDown, 1200))
    }
}
