package app.libreshot.capture

/**
 * Routes launcher-intent arrivals: a plain icon tap opens the app, trusted callers
 * capture. Heuristic only - EXTRA_REFERRER is spoofable - so capture stays bounded
 * by CaptureActivity's delay clamp, and screenshots stay in-process regardless.
 */
object LaunchRouter {

    enum class Action { ONBOARDING, CAPTURE, OPEN_SETTINGS }

    private val SYSTEMUI_PACKAGES = setOf(
        "com.android.systemui",
        "com.google.android.systemui",
    )

    fun decide(serviceRunning: Boolean, hasSourceExtra: Boolean, referrerHost: String?): Action = when {
        !serviceRunning -> Action.ONBOARDING
        hasSourceExtra -> Action.CAPTURE
        referrerHost in SYSTEMUI_PACKAGES -> Action.CAPTURE
        else -> Action.OPEN_SETTINGS
    }
}
