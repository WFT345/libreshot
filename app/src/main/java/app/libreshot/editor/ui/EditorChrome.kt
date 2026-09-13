package app.libreshot.editor.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.libreshot.R
import app.libreshot.editor.model.Tool
import app.libreshot.ui.icons.AppIcons
import app.libreshot.ui.theme.Ios

@Composable
fun EditorTopBar(
    canUndo: Boolean,
    canRedo: Boolean,
    onDone: () -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .widthIn(min = 88.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(onClick = onDone),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                stringResource(R.string.done),
                color = Ios.Accent,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onUndo, enabled = canUndo) {
            Icon(
                AppIcons.Undo,
                contentDescription = null,
                tint = if (canUndo) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onRedo, enabled = canRedo) {
            Icon(
                AppIcons.Redo,
                contentDescription = null,
                tint = if (canRedo) Color.White else Color.White.copy(alpha = 0.3f),
            )
        }
        IconButton(onClick = onShare) {
            Icon(AppIcons.Share, contentDescription = null, tint = Color.White)
        }
    }
}

@Composable
fun ToolPalette(selected: Tool, onSelect: (Tool) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(64.dp)
            .padding(horizontal = 32.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PaletteButton(AppIcons.Pen, selected == Tool.PEN) { onSelect(Tool.PEN) }
        PaletteButton(AppIcons.Marker, selected == Tool.MARKER) { onSelect(Tool.MARKER) }
        PaletteButton(AppIcons.Pencil, selected == Tool.PENCIL) { onSelect(Tool.PENCIL) }
        PaletteButton(AppIcons.Eraser, selected == Tool.ERASER) { onSelect(Tool.ERASER) }
    }
}

@Composable
private fun PaletteButton(icon: ImageVector, selected: Boolean, onClick: () -> Unit) {
    val lift by animateDpAsState(if (selected) (-8).dp else 0.dp, tween(150), label = "lift")
    Box(
        Modifier
            .size(44.dp)
            .graphicsLayer { translationY = lift.toPx() }
            .clip(CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = if (selected) Ios.Accent else Color.White)
    }
}

private val PRESET_COLORS = listOf(
    Color.White,
    Color.Black,
    Color(0xFFFF453A),
    Color(0xFFFFD60A),
    Color(0xFF30D158),
    Color(0xFF0A84FF),
)

@Composable
fun ColorRow(
    visible: Boolean,
    selected: Color,
    onSelect: (Color) -> Unit,
    onOpenPicker: () -> Unit,
) {
    AnimatedVisibility(visible, enter = expandVertically(), exit = shrinkVertically()) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PRESET_COLORS.forEach { color ->
                val isSelected = selected == color
                Box(
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Ios.Accent else Color.White.copy(alpha = 0.35f),
                            shape = CircleShape,
                        )
                        .clickable { onSelect(color) },
                )
            }
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.sweepGradient(
                            listOf(
                                Color(0xFFFF3B30),
                                Color(0xFFFFCC00),
                                Color(0xFF34C759),
                                Color(0xFF32ADE6),
                                Color(0xFF5856D6),
                                Color(0xFFAF52DE),
                                Color(0xFFFF3B30),
                            ),
                        ),
                    )
                    .clickable(onClick = onOpenPicker),
            )
        }
    }
}
