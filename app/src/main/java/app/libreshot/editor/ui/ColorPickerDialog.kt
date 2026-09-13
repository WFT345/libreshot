package app.libreshot.editor.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.libreshot.R
import app.libreshot.ui.theme.Ios

/** HSV square + hue bar + opacity slider, in the spirit of the iOS markup colour picker. */
@Composable
fun ColorPickerDialog(
    initial: Color,
    onPick: (Color) -> Unit,
    onDismiss: () -> Unit,
) {
    var hsv by remember {
        mutableStateOf(FloatArray(3).also { android.graphics.Color.colorToHSV(initial.toArgb(), it) })
    }
    var opacity by remember { mutableStateOf(initial.alpha) }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .clip(RoundedCornerShape(13.dp))
                .background(Ios.Surface)
                .padding(16.dp),
        ) {
            val current = Color.hsv(hsv[0], hsv[1], hsv[2]).copy(alpha = opacity)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(current),
            )
            Spacer(Modifier.height(12.dp))

            PickerArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .clip(RoundedCornerShape(8.dp)),
                onInput = { fx, fy -> hsv = floatArrayOf(hsv[0], fx, 1f - fy) },
            ) {
                drawRect(
                    Brush.horizontalGradient(
                        listOf(Color.White, Color.hsv(hsv[0], 1f, 1f)),
                    ),
                )
                drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color.Black)))
                val dot = Offset(hsv[1] * size.width, (1f - hsv[2]) * size.height)
                drawCircle(Color.White, radius = 10.dp.toPx(), center = dot, style = Stroke(2.dp.toPx()))
                drawCircle(current, radius = 8.dp.toPx(), center = dot)
            }
            Spacer(Modifier.height(12.dp))

            PickerArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp)),
                onInput = { fx, _ -> hsv = floatArrayOf(fx * 360f, hsv[1], hsv[2]) },
            ) {
                drawRect(
                    Brush.horizontalGradient(
                        (0..6).map { Color.hsv(it * 60f, 1f, 1f) },
                    ),
                )
                val x = hsv[0] / 360f * size.width
                drawRoundRect(
                    Color.White,
                    topLeft = Offset(x - 2.dp.toPx(), 0f),
                    size = Size(4.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
            Spacer(Modifier.height(12.dp))

            PickerArea(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp)),
                onInput = { fx, _ -> opacity = fx.coerceIn(0.05f, 1f) },
            ) {
                drawRect(Ios.Background)
                drawRect(
                    Brush.horizontalGradient(
                        listOf(
                            Color.hsv(hsv[0], hsv[1], hsv[2]).copy(alpha = 0f),
                            Color.hsv(hsv[0], hsv[1], hsv[2]),
                        ),
                    ),
                )
                val x = opacity * size.width
                drawRoundRect(
                    Color.White,
                    topLeft = Offset(x - 2.dp.toPx(), 0f),
                    size = Size(4.dp.toPx(), size.height),
                    cornerRadius = CornerRadius(2.dp.toPx()),
                )
            }
            Spacer(Modifier.height(8.dp))

            Row {
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel), color = Ios.SecondaryText)
                }
                TextButton(onClick = { onPick(current) }) {
                    Text(stringResource(R.string.done), color = Ios.Accent)
                }
            }
        }
    }
}

@Composable
private fun PickerArea(
    modifier: Modifier,
    onInput: (fractionX: Float, fractionY: Float) -> Unit,
    content: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit,
) {
    Box(
        modifier.pointerInput(Unit) {
            awaitEachGesture {
                val down = awaitFirstDown(requireUnconsumed = false)
                fun handle(position: Offset) = onInput(
                    (position.x / size.width).coerceIn(0f, 1f),
                    (position.y / size.height).coerceIn(0f, 1f),
                )
                handle(down.position)
                down.consume()
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.pressed } ?: break
                    handle(change.position)
                    change.consume()
                }
            }
        },
    ) {
        Canvas(Modifier.matchParentSize()) {
            content()
        }
    }
}
