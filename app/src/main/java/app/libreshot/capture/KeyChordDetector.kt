package app.libreshot.capture

import android.view.KeyEvent

/** Matches a Volume Down double-press within [windowMs]. Pure logic for unit tests. */
class KeyChordDetector(private val windowMs: Long = 350L) {

    private var lastPressAt = 0L

    /** Returns true when this key-down completes the chord. */
    fun onKeyDown(keyCode: Int, eventTimeMs: Long): Boolean {
        if (keyCode != KeyEvent.KEYCODE_VOLUME_DOWN) {
            lastPressAt = 0L
            return false
        }
        val matched = lastPressAt != 0L && eventTimeMs - lastPressAt in 1..windowMs
        lastPressAt = if (matched) 0L else eventTimeMs
        return matched
    }
}
