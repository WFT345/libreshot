package app.libreshot.capture

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import app.libreshot.onboarding.OnboardingActivity
import app.libreshot.session.CaptureSource
import app.libreshot.settings.SettingsActivity

/**
 * Transparent trampoline behind the launcher icon. The QS tile and the long-press
 * shortcut (via [EXTRA_SOURCE]) and SystemUI launches (Quick Tap, side key) trigger
 * a capture; a plain icon tap opens Settings. The window is invisible and lives for
 * a few milliseconds.
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
        val sourceName = intent.getStringExtra(EXTRA_SOURCE)
        when (LaunchRouter.decide(
            serviceRunning = CaptureService.instance != null,
            hasSourceExtra = sourceName != null,
            referrerHost = referrer?.host,
        )) {
            LaunchRouter.Action.ONBOARDING ->
                startActivity(Intent(this, OnboardingActivity::class.java))
            LaunchRouter.Action.OPEN_SETTINGS ->
                startActivity(Intent(this, SettingsActivity::class.java))
            LaunchRouter.Action.CAPTURE -> {
                val source = sourceName
                    ?.let { name -> CaptureSource.entries.firstOrNull { it.name == name } }
                    ?: CaptureSource.QUICK_TAP
                CaptureService.instance
                    ?.requestCapture(source, clampDelay(intent.getLongExtra(EXTRA_DELAY_MS, 0L)))
            }
        }
        finish()
        @Suppress("DEPRECATION")
        overridePendingTransition(0, 0)
    }
}
