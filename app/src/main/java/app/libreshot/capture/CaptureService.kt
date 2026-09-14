package app.libreshot.capture

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.Display
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import app.libreshot.R
import app.libreshot.editor.EditorActivity
import app.libreshot.overlay.ThumbnailOverlay
import app.libreshot.session.CaptureSource
import app.libreshot.session.ScreenshotSession
import app.libreshot.session.SessionStore
import app.libreshot.settings.Prefs
import app.libreshot.settings.PrefsSnapshot
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class CaptureService : AccessibilityService() {

    companion object {
        @Volatile
        var instance: CaptureService? = null
            private set

        // Give SurfaceFlinger a few frames to drop our overlay before it composites the capture.
        private const val SETTLE_AFTER_HIDE_MS = 100L
    }

    private val handler = Handler(Looper.getMainLooper())
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val chordDetector = KeyChordDetector()
    private val rateLimiter = RateLimiter(windowMs = 400L)
    private var backTapMonitor: BackTapMonitor? = null
    private var overlay: ThumbnailOverlay? = null
    private var prefs = PrefsSnapshot()

    override fun onServiceConnected() {
        instance = this
        val overlay = ThumbnailOverlay(this)
        this.overlay = overlay
        backTapMonitor = BackTapMonitor(this) {
            // Screenshot-on-lockscreen is deliberately excluded from back-tap.
            if (getSystemService(KeyguardManager::class.java)?.isKeyguardLocked != true) {
                requestCapture(CaptureSource.BACK_TAP)
            }
        }
        scope.launch {
            Prefs.flow(this@CaptureService).collect { value ->
                val chordToggled = value.volumeChord != prefs.volumeChord
                prefs = value
                overlay.prefs = value
                if (chordToggled) applyKeyEventFilter()
                backTapMonitor?.setThreshold(value.backTapThreshold)
                backTapMonitor?.setEnabled(value.backTap)
            }
        }
        CaptureTile.requestListening(this)
    }

    override fun onUnbind(intent: Intent?): Boolean {
        backTapMonitor?.destroy()
        backTapMonitor = null
        overlay?.destroy()
        overlay = null
        instance = null
        scope.cancel()
        CaptureTile.requestListening(this)
        return super.onUnbind(intent)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = Unit

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (!prefs.volumeChord) return super.onKeyEvent(event)
        if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0 &&
            chordDetector.onKeyDown(event.keyCode, event.eventTime)
        ) {
            requestCapture(CaptureSource.CHORD)
        }
        // Swallow volume-down so the chord never changes volume; everything else passes.
        return if (event.keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) true else super.onKeyEvent(event)
    }

    fun requestCapture(source: CaptureSource, delayMs: Long = 0L) {
        handler.postDelayed({ capture(source) }, delayMs)
    }

    private fun capture(source: CaptureSource) {
        if (!rateLimiter.allow(SystemClock.uptimeMillis())) return

        val overlayWasShowing = overlay?.isShowing == true
        overlay?.hide()
        handler.postDelayed({
            takeScreenshot(Display.DEFAULT_DISPLAY, mainExecutor, ScreenshotCallback(source))
        }, if (overlayWasShowing) SETTLE_AFTER_HIDE_MS else 0L)
    }

    private fun present(session: ScreenshotSession) {
        if (prefs.showThumbnail) {
            overlay?.show(session)
        } else {
            openEditor(session.id)
        }
    }

    fun openEditor(sessionId: String) {
        val intent = EditorActivity.intent(this, sessionId).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            startActivity(intent)
        } catch (e: RuntimeException) {
            toast(R.string.capture_failed)
        }
    }

    private fun applyKeyEventFilter() {
        val info = serviceInfo ?: return
        val enabled = info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS != 0
        if (enabled == prefs.volumeChord) return
        info.flags = if (prefs.volumeChord) {
            info.flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        } else {
            info.flags and AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS.inv()
        }
        setServiceInfo(info)
    }

    private fun toast(messageRes: Int) {
        Toast.makeText(this, messageRes, Toast.LENGTH_SHORT).show()
    }

    private inner class ScreenshotCallback(private val source: CaptureSource) : TakeScreenshotCallback {
        override fun onSuccess(result: ScreenshotResult) {
            val hardware = Bitmap.wrapHardwareBuffer(result.hardwareBuffer, result.colorSpace)
            result.hardwareBuffer.close()
            val bitmap = hardware?.copy(Bitmap.Config.ARGB_8888, false)
            hardware?.recycle()
            if (bitmap == null) {
                toast(R.string.capture_failed)
                return
            }
            val session = ScreenshotSession(
                id = UUID.randomUUID().toString(),
                bitmap = bitmap,
                capturedAt = System.currentTimeMillis(),
                source = source,
            )
            SessionStore.put(session)
            present(session)
        }

        override fun onFailure(errorCode: Int) {
            when (errorCode) {
                ERROR_TAKE_SCREENSHOT_SECURE_WINDOW -> toast(R.string.capture_blocked)
                ERROR_TAKE_SCREENSHOT_INTERVAL_TIME_SHORT -> Unit
                else -> toast(R.string.capture_failed)
            }
        }
    }
}
