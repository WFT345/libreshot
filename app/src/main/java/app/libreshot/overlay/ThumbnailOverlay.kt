package app.libreshot.overlay

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.GestureDetector
import android.view.Gravity
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.WindowInsets
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
import android.view.WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
import android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
import android.view.WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
import android.view.WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
import android.view.animation.PathInterpolator
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.IntRect
import app.libreshot.capture.CaptureService
import app.libreshot.editor.EditorActivity
import app.libreshot.session.ScreenshotSession
import app.libreshot.settings.PrefsSnapshot
import app.libreshot.share.ShareHelper
import app.libreshot.ui.theme.LibreShotTheme
import kotlin.math.abs
import kotlin.math.roundToInt

class ThumbnailOverlay(private val service: CaptureService) {

    class Geometry(
        val screenW: Int,
        val screenH: Int,
        val thumbW: Int,
        val thumbH: Int,
        val thumbLeft: Int,
        val thumbTop: Int,
        val bottomMargin: Int,
        val marginPx: Int,
        val shadowPad: Int,
        val cornerLeft: Boolean,
    ) {
        val edgeSign: Float get() = if (cornerLeft) -1f else 1f
        val rect: IntRect get() = IntRect(thumbLeft, thumbTop, thumbLeft + thumbW, thumbTop + thumbH)
        val windowW: Int get() = thumbW + shadowPad * 2
        val windowRestX: Int get() = thumbLeft - shadowPad
        val windowY: Int get() = bottomMargin - shadowPad
        val exitX: Int get() = if (cornerLeft) -windowW else screenW + shadowPad
    }

    var prefs = PrefsSnapshot()

    val isShowing: Boolean get() = flashHost != null || thumbHost != null
    var thumbRect: IntRect? = null
        private set

    private val windowManager = service.getSystemService(WindowManager::class.java)
    private val handler = Handler(Looper.getMainLooper())

    private var flashHost: ComposeView? = null
    private var flashOwner: OverlayLifecycleOwner? = null
    private var thumbHost: ComposeView? = null
    private var thumbOwner: OverlayLifecycleOwner? = null
    private var thumbParams: WindowManager.LayoutParams? = null

    private val pressScale = mutableFloatStateOf(1f)
    private var dragOffset = 0f
    private var interacting = false
    private var autoDismiss: Runnable? = null
    private var pendingHide: Runnable? = null
    private var xAnimator: ValueAnimator? = null

    fun show(session: ScreenshotSession) {
        removeWindows()
        val geom = computeGeometry()
        thumbRect = geom.rect
        attachFlashWindow(session, geom)
        if (prefs.haptic) {
            flashHost?.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }
    }

    fun hide() = removeWindows()

    fun destroy() = removeWindows()

    private fun computeGeometry(): Geometry {
        val metrics = windowManager.currentWindowMetrics
        val bounds = metrics.bounds
        val navBottom = metrics.windowInsets
            .getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars())
            .bottom
        val density = service.resources.displayMetrics.density
        val margin = (16 * density).roundToInt()
        val thumbH = (120 * density).roundToInt()
        val thumbW = (thumbH * bounds.width() / bounds.height().toFloat()).roundToInt()
        val left = if (prefs.cornerLeft) margin else bounds.width() - margin - thumbW
        val top = bounds.height() - navBottom - margin - thumbH
        return Geometry(
            screenW = bounds.width(),
            screenH = bounds.height(),
            thumbW = thumbW,
            thumbH = thumbH,
            thumbLeft = left,
            thumbTop = top,
            bottomMargin = navBottom + margin,
            marginPx = margin,
            shadowPad = (8 * density).roundToInt(),
            cornerLeft = prefs.cornerLeft,
        )
    }

    private fun attachFlashWindow(session: ScreenshotSession, geom: Geometry) {
        val owner = OverlayLifecycleOwner()
        val view = ComposeView(service)
        owner.attachTo(view)
        view.setContent {
            LibreShotTheme {
                ThumbnailEnter(session, geom, onDone = { onEnterDone(session, geom) })
            }
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            TYPE_ACCESSIBILITY_OVERLAY,
            FLAG_NOT_TOUCHABLE or FLAG_NOT_FOCUSABLE or FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT,
        )
        windowManager.addView(view, params)
        owner.onWindowShown()
        flashHost = view
        flashOwner = owner
    }

    private fun onEnterDone(session: ScreenshotSession, geom: Geometry) {
        removeFlashWindow()
        attachThumbWindow(session, geom)
        scheduleAutoDismiss()
    }

    private fun attachThumbWindow(session: ScreenshotSession, geom: Geometry) {
        pressScale.floatValue = 1f
        dragOffset = 0f
        val owner = OverlayLifecycleOwner()
        val view = ComposeView(service)
        owner.attachTo(view)
        view.setContent {
            LibreShotTheme {
                ThumbnailSettled(session = session, geom = geom, pressScale = pressScale)
            }
        }
        view.setOnTouchListener(ThumbTouchListener(session, geom, view))
        val params = WindowManager.LayoutParams(
            geom.windowW,
            geom.thumbH + geom.shadowPad * 2,
            TYPE_ACCESSIBILITY_OVERLAY,
            FLAG_NOT_TOUCH_MODAL or FLAG_NOT_FOCUSABLE or
                FLAG_LAYOUT_IN_SCREEN or FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT,
        ).apply {
            gravity = Gravity.BOTTOM or Gravity.START
            x = geom.windowRestX
            y = geom.windowY
        }
        windowManager.addView(view, params)
        owner.onWindowShown()
        thumbHost = view
        thumbOwner = owner
        thumbParams = params
    }

    private inner class ThumbTouchListener(
        private val session: ScreenshotSession,
        private val geom: Geometry,
        private val view: View,
    ) : View.OnTouchListener {

        private val touchSlop = ViewConfiguration.get(service).scaledTouchSlop
        private val flingPx = 600 * service.resources.displayMetrics.density
        private var downRawX = 0f
        private var lastRawX = 0f
        private var lastTime = 0L
        private var velocity = 0f
        private var dragging = false

        private val gestures = GestureDetector(service, object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapUp(e: MotionEvent): Boolean {
                animatePressScale(1f)
                openEditor(session, geom)
                return true
            }

            override fun onLongPress(e: MotionEvent) {
                if (dragging) return
                interacting = false
                animatePressScale(1f)
                view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                share(session)
            }
        })

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, event: MotionEvent): Boolean {
            gestures.onTouchEvent(event)
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    interacting = true
                    xAnimator?.cancel()
                    dragging = false
                    downRawX = event.rawX
                    lastRawX = event.rawX
                    lastTime = event.eventTime
                    velocity = 0f
                    animatePressScale(0.95f)
                }

                MotionEvent.ACTION_MOVE -> {
                    val dt = (event.eventTime - lastTime).coerceAtLeast(1L)
                    val dx = event.rawX - lastRawX
                    velocity = 0.9f * velocity + 0.1f * (dx / dt * 1000f)
                    lastRawX = event.rawX
                    lastTime = event.eventTime
                    if (!dragging && abs(event.rawX - downRawX) > touchSlop) {
                        dragging = true
                        animatePressScale(1f)
                    }
                    if (dragging) {
                        val next = dragOffset + dx
                        dragOffset =
                            if (geom.edgeSign < 0) next.coerceAtMost(0f) else next.coerceAtLeast(0f)
                        applyWindowX(geom)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    interacting = false
                    if (dragging) {
                        val towardEdge = dragOffset * geom.edgeSign
                        val velocityTowardEdge = velocity * geom.edgeSign
                        if (towardEdge > DISMISS_DISTANCE_FRACTION * geom.thumbW ||
                            velocityTowardEdge > flingPx
                        ) {
                            animateExit(geom)
                        } else {
                            animateWindowX(geom, toX = geom.windowRestX, durationMs = 250, springy = true)
                        }
                    } else {
                        animatePressScale(1f)
                    }
                }
            }
            return true
        }
    }

    private fun applyWindowX(geom: Geometry) {
        val host = thumbHost ?: return
        val params = thumbParams ?: return
        params.x = (geom.windowRestX + dragOffset).roundToInt()
        windowManager.updateViewLayout(host, params)
    }

    private fun animateWindowX(geom: Geometry, toX: Int, durationMs: Long, springy: Boolean) {
        val host = thumbHost ?: return
        val params = thumbParams ?: return
        val fromX = params.x
        xAnimator?.cancel()
        xAnimator = ValueAnimator.ofInt(fromX, toX).apply {
            duration = durationMs
            interpolator = if (springy) {
                PathInterpolator(0.175f, 0.885f, 0.32f, 1.1f)
            } else {
                PathInterpolator(0.4f, 0f, 1f, 1f)
            }
            addUpdateListener {
                params.x = it.animatedValue as Int
                windowManager.updateViewLayout(host, params)
            }
            start()
        }
        dragOffset = (toX - geom.windowRestX).toFloat()
        if (toX == geom.exitX) {
            val run = Runnable { hide() }
            pendingHide = run
            handler.postDelayed(run, durationMs)
        }
    }

    private fun animateExit(geom: Geometry) {
        animateWindowX(geom, toX = geom.exitX, durationMs = 300, springy = false)
    }

    private fun animatePressScale(target: Float) {
        val from = pressScale.floatValue
        if (from == target) return
        ValueAnimator.ofFloat(from, target).apply {
            duration = 80
            addUpdateListener { pressScale.floatValue = it.animatedValue as Float }
            start()
        }
    }

    private fun scheduleAutoDismiss() {
        val runnable = Runnable {
            if (interacting) {
                scheduleAutoDismiss()
            } else {
                val geom = computeGeometry()
                animateExit(geom)
            }
        }
        autoDismiss = runnable
        handler.postDelayed(runnable, prefs.thumbDurationMs)
    }

    private fun openEditor(session: ScreenshotSession, geom: Geometry) {
        val intent = EditorActivity.intent(service, session.id)
            .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        val host = thumbHost
        try {
            if (host != null) {
                val rect = geom.rect
                val options = ActivityOptions.makeScaleUpAnimation(
                    host, rect.left, rect.top, rect.width, rect.height,
                )
                service.startActivity(intent, options.toBundle())
            } else {
                service.startActivity(intent)
            }
        } catch (e: SecurityException) {
            service.startActivity(intent)
        }
        hide()
    }

    private fun share(session: ScreenshotSession) {
        ShareHelper.share(service, session, edited = false)
        hide()
    }

    private fun removeFlashWindow() {
        flashHost?.let { windowManager.removeViewImmediate(it) }
        flashOwner?.destroy()
        flashHost = null
        flashOwner = null
    }

    private fun removeThumbWindow() {
        thumbHost?.let { windowManager.removeViewImmediate(it) }
        thumbOwner?.destroy()
        thumbHost = null
        thumbOwner = null
        thumbParams = null
    }

    private fun removeWindows() {
        autoDismiss?.let { handler.removeCallbacks(it) }
        autoDismiss = null
        pendingHide?.let { handler.removeCallbacks(it) }
        pendingHide = null
        xAnimator?.cancel()
        xAnimator = null
        interacting = false
        removeFlashWindow()
        removeThumbWindow()
    }

    private companion object {
        const val DISMISS_DISTANCE_FRACTION = 0.4f
    }
}
