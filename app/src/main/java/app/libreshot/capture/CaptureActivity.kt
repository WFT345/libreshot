package app.libreshot.capture

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import app.libreshot.onboarding.OnboardingActivity
import app.libreshot.session.CaptureSource

/**
 * Transparent trampoline that Quick Tap (Pixel "Open app") and the Quick Settings tile launch.
 * The window is invisible and lives for a few milliseconds.
 */
class CaptureActivity : Activity() {

    companion object {
        const val EXTRA_DELAY_MS = "app.libreshot.extra.DELAY_MS"
        const val EXTRA_SOURCE = "app.libreshot.extra.SOURCE"

        /** The activity is exported; only the tile's 400 ms shade-collapse delay is a real caller. */
        const val MAX_CAPTURE_DELAY_MS = 2_000L

        fun clampDelay(delayMs: Long): Long = delayMs.coerceIn(0L, MAX_CAPTURE_DELAY_MS)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val service = CaptureService.instance
        if (service == null) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        } else {
            val source = intent.getStringExtra(EXTRA_SOURCE)
                ?.let { name -> CaptureSource.entries.firstOrNull { it.name == name } }
                ?: CaptureSource.QUICK_TAP
            service.requestCapture(source, clampDelay(intent.getLongExtra(EXTRA_DELAY_MS, 0L)))
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
