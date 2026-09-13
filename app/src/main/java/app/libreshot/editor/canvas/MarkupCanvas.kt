package app.libreshot.editor.canvas

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import app.libreshot.editor.model.Stroke
import app.libreshot.editor.model.Tool
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_ZOOM = 4f

/**
 * One finger draws with the active tool, two fingers always pan/zoom (cancelling any stroke
 * in progress), three-finger horizontal swipe is undo/redo.
 */
@Composable
fun MarkupCanvas(
    bitmap: Bitmap,
    strokes: List<Stroke>,
    liveStroke: Stroke?,
    tool: Tool,
    onBeginStroke: (at: Offset, widthPx: Float) -> Unit,
    onExtendStroke: (at: Offset) -> Unit,
    onEndStroke: () -> Unit,
    onCancelStroke: () -> Unit,
    onErase: (at: Offset, tolerancePx: Float) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = androidx.compose.ui.platform.LocalDensity.current
        val wPx = constraints.maxWidth.toFloat()
        val hPx = constraints.maxHeight.toFloat()
        val fitScale = min(wPx / bitmap.width, hPx / bitmap.height)
        val origin = Offset(
            (wPx - bitmap.width * fitScale) / 2f,
            (hPx - bitmap.height * fitScale) / 2f,
        )
        var zoom by remember { mutableStateOf(1f) }
        var pan by remember { mutableStateOf(Offset.Zero) }
        val image = remember { bitmap.asImageBitmap() }

        val eraseTolerancePx = with(density) { 12.dp.toPx() }

        fun screenToBitmap(p: Offset): Offset = Offset(
            ((p.x - pan.x) / zoom - origin.x) / fitScale,
            ((p.y - pan.y) / zoom - origin.y) / fitScale,
        )

        fun widthPxFor(t: Tool): Float = with(density) {
            // The bitmap is captured at native resolution, so screen px == bitmap px.
            when (t) {
                Tool.PEN -> 4.dp.toPx()
                Tool.MARKER -> 18.dp.toPx()
                Tool.PENCIL -> 2.5.dp.toPx()
                Tool.ERASER -> 24.dp.toPx()
            }
        }

        fun transform(centroid: Offset, zoomChange: Float, panChange: Offset) {
            val old = zoom
            val next = (old * zoomChange).coerceIn(1f, MAX_ZOOM)
            val applied = next / old
            val unclamped = Offset(
                (pan.x - centroid.x) * applied + centroid.x + panChange.x,
                (pan.y - centroid.y) * applied + centroid.y + panChange.y,
            )
            pan = Offset(
                unclamped.x.coerceIn(wPx * (1f - next), 0f),
                unclamped.y.coerceIn(hPx * (1f - next), 0f),
            )
            zoom = next
        }

        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(tool) {
                    val threeFingerThreshold = 60.dp.toPx()
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var strokeActive = false
                        var transforming = false
                        var threeFingerFired = false
                        var accPanX = 0f
                        if (tool == Tool.ERASER) {
                            onErase(screenToBitmap(down.position), eraseTolerancePx)
                        } else {
                            onBeginStroke(screenToBitmap(down.position), widthPxFor(tool))
                            strokeActive = true
                        }
                        while (true) {
                            val event = awaitPointerEvent()
                            val pressed = event.changes.filter { it.pressed }
                            when {
                                pressed.size >= 3 -> {
                                    if (strokeActive) {
                                        onCancelStroke()
                                        strokeActive = false
                                    }
                                    accPanX += event.calculatePan().x
                                    if (!threeFingerFired && abs(accPanX) > threeFingerThreshold) {
                                        threeFingerFired = true
                                        if (accPanX < 0) onUndo() else onRedo()
                                    }
                                    event.changes.forEach { it.consume() }
                                }

                                pressed.size == 2 -> {
                                    if (strokeActive) {
                                        onCancelStroke()
                                        strokeActive = false
                                    }
                                    transforming = true
                                    transform(
                                        event.calculateCentroid(),
                                        event.calculateZoom(),
                                        event.calculatePan(),
                                    )
                                    event.changes.forEach { if (it.positionChanged()) it.consume() }
                                }

                                pressed.size == 1 -> {
                                    val change = pressed[0]
                                    when {
                                        strokeActive -> if (change.positionChanged()) {
                                            onExtendStroke(screenToBitmap(change.position))
                                            change.consume()
                                        }

                                        transforming -> {
                                            transform(change.position, 1f, change.positionChange())
                                            change.consume()
                                        }

                                        tool == Tool.ERASER -> {
                                            onErase(
                                                screenToBitmap(change.position),
                                                eraseTolerancePx,
                                            )
                                            change.consume()
                                        }
                                    }
                                }
                            }
                            if (pressed.isEmpty()) {
                                if (strokeActive) onEndStroke()
                                break
                            }
                        }
                    }
                },
        ) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = zoom
                        scaleY = zoom
                        translationX = pan.x
                        translationY = pan.y
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
                drawImage(
                    image = image,
                    srcOffset = IntOffset.Zero,
                    srcSize = IntSize(image.width, image.height),
                    dstOffset = IntOffset(origin.x.roundToInt(), origin.y.roundToInt()),
                    dstSize = IntSize(
                        (bitmap.width * fitScale).roundToInt(),
                        (bitmap.height * fitScale).roundToInt(),
                    ),
                    filterQuality = FilterQuality.Medium,
                )
                strokes.forEach { drawStroke(it, fitScale, origin) }
                liveStroke?.let { drawStroke(it, fitScale, origin) }
            }
        }
    }
}
