package app.libreshot.overlay

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.libreshot.session.ScreenshotSession

private const val ENTER_MS = 450
private const val FLASH_MS = 200

/**
 * Full-screen, untouchable enter animation: the capture shrinks from full size into the
 * corner slot while a white flash fades. Calls [onDone] when settled; the interactive
 * thumbnail window replaces this one.
 */
@Composable
fun ThumbnailEnter(
    session: ScreenshotSession,
    geom: ThumbnailOverlay.Geometry,
    onDone: () -> Unit,
) {
    val image = remember(session.id) { session.bitmap.asImageBitmap() }
    val progress = remember { Animatable(0f) }
    val flash = remember { Animatable(0.8f) }

    LaunchedEffect(Unit) {
        flash.animateTo(0f, tween(FLASH_MS))
    }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(ENTER_MS, easing = FastOutSlowInEasing))
        onDone()
    }

    Box(Modifier.fillMaxSize()) {
        val p = progress.value
        val scale = 1f + (geom.thumbH / geom.screenH.toFloat() - 1f) * p
        val tx = (if (geom.cornerLeft) geom.thumbLeft
            else geom.thumbLeft + geom.thumbW - geom.screenW) * p
        val ty = -(geom.screenH - geom.thumbTop - geom.thumbH) * p
        val origin = if (geom.cornerLeft) TransformOrigin(0f, 1f) else TransformOrigin(1f, 1f)
        Image(
            bitmap = image,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = tx
                    translationY = ty
                    transformOrigin = origin
                },
        )
        if (flash.value > 0.01f) {
            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = flash.value }
                    .background(Color.White),
            )
        }
    }
}

/**
 * Visual-only settled thumbnail. Gestures live in the overlay's View touch listener, which
 * moves the window itself; this composable renders the card with its press-scale feedback.
 */
@Composable
fun ThumbnailSettled(
    session: ScreenshotSession,
    geom: ThumbnailOverlay.Geometry,
    pressScale: State<Float>,
) {
    val density = LocalDensity.current
    val image = remember(session.id) { session.bitmap.asImageBitmap() }
    Box(
        Modifier
            .fillMaxSize()
            .padding(with(density) { geom.shadowPad.toDp() }),
        contentAlignment = if (geom.cornerLeft) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        Box(
            Modifier
                .size(
                    width = with(density) { geom.thumbW.toDp() },
                    height = with(density) { geom.thumbH.toDp() },
                )
                .graphicsLayer {
                    scaleX = pressScale.value
                    scaleY = pressScale.value
                }
                .shadow(8.dp, RoundedCornerShape(8.dp))
                .border(2.dp, Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp)),
        ) {
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
