package app.libreshot.capture

/**
 * Routes launcher-intent arrivals: a plain icon tap opens the app; the QS tile, the
 * Capture shortcut, and SystemUI (Quick Tap, side key) capture. This is convenience
 * routing, not authorization. Any app can set the source extra, and EXTRA_REFERRER
 * is caller-supplied. The enforced bounds live elsewhere: the delay clamp, the rate
 * limit, the keyguard gate, and captures never leaving the process.
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
