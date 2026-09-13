package app.libreshot.editor.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.libreshot.R
import app.libreshot.ui.theme.Ios

enum class DoneAction { SAVE, COPY, COPY_DELETE, DELETE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DoneSheet(onAction: (DoneAction) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Ios.Background,
        dragHandle = null,
        shape = RoundedCornerShape(topStart = 13.dp, topEnd = 13.dp),
    ) {
        Column(Modifier.padding(horizontal = 12.dp)) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Ios.Surface),
            ) {
                SheetRow(stringResource(R.string.save_to_photos)) { onAction(DoneAction.SAVE) }
                SheetDivider()
                SheetRow(stringResource(R.string.copy)) { onAction(DoneAction.COPY) }
                SheetDivider()
                SheetRow(stringResource(R.string.copy_and_delete)) { onAction(DoneAction.COPY_DELETE) }
                SheetDivider()
                SheetRow(stringResource(R.string.delete_screenshot), Ios.Destructive) {
                    onAction(DoneAction.DELETE)
                }
            }
            Spacer(Modifier.height(8.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(13.dp))
                    .background(Ios.Surface),
            ) {
                SheetRow(stringResource(R.string.cancel)) { onDismiss() }
            }
            Spacer(Modifier.height(8.dp))
            Spacer(Modifier.navigationBarsPadding())
        }
    }
}

@Composable
private fun SheetRow(label: String, color: Color = Ios.PrimaryText, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = color, fontSize = 17.sp)
    }
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(thickness = 0.5.dp, color = Ios.Separator)
}
