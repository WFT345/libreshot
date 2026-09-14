package app.libreshot.editor

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import app.libreshot.R
import app.libreshot.editor.canvas.MarkupCanvas
import app.libreshot.editor.model.Tool
import app.libreshot.editor.ui.ColorPickerDialog
import app.libreshot.editor.ui.ColorRow
import app.libreshot.editor.ui.DoneAction
import app.libreshot.editor.ui.DoneSheet
import app.libreshot.editor.ui.EditorTopBar
import app.libreshot.editor.ui.ToolPalette
import app.libreshot.export.Compositor
import app.libreshot.export.MediaStoreSaver
import app.libreshot.session.ScreenshotSession
import app.libreshot.session.SessionStore
import app.libreshot.share.ClipboardCopier
import app.libreshot.share.ShareHelper
import app.libreshot.ui.theme.Ios
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun EditorScreen(session: ScreenshotSession, onFinish: () -> Unit) {
    val vm: EditorViewModel = viewModel(factory = EditorViewModel.factory(session))
    val strokes by vm.strokes.collectAsStateWithLifecycle()
    val liveStroke by vm.liveStroke.collectAsStateWithLifecycle()
    val canUndo by vm.canUndo.collectAsStateWithLifecycle()
    val canRedo by vm.canRedo.collectAsStateWithLifecycle()
    val hasEdits by vm.hasEdits.collectAsStateWithLifecycle()
    val tool by vm.tool.collectAsStateWithLifecycle()
    val color by vm.color.collectAsStateWithLifecycle()
    val crop by vm.crop.collectAsStateWithLifecycle()

    var showDone by rememberSaveable { mutableStateOf(false) }
    var showColorPicker by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    BackHandler {
        if (hasEdits) showDone = true else onFinish()
    }

    Column(Modifier.fillMaxSize().background(Ios.Background)) {
        EditorTopBar(
            canUndo = canUndo,
            canRedo = canRedo,
            onDone = { showDone = true },
            onUndo = vm::undo,
            onRedo = vm::redo,
            onShare = { ShareHelper.share(context, session, edited = true) },
        )
        Box(Modifier.weight(1f)) {
            MarkupCanvas(
                bitmap = session.bitmap,
                strokes = strokes,
                liveStroke = liveStroke,
                crop = crop,
                tool = tool,
                onBeginStroke = vm::beginStroke,
                onExtendStroke = vm::extendStroke,
                onEndStroke = vm::endStroke,
                onCancelStroke = vm::cancelStroke,
                onErase = vm::eraseAt,
                onBeginCropDrag = vm::beginCropDrag,
                onDragCrop = vm::dragCrop,
                onEndCropDrag = vm::endCropDrag,
                onCancelCropDrag = vm::cancelCropDrag,
                onUndo = vm::undo,
                onRedo = vm::redo,
            )
        }
        ColorRow(
            visible = tool != Tool.ERASER,
            selected = color,
            onSelect = vm::setColor,
            onOpenPicker = { showColorPicker = true },
        )
        ToolPalette(selected = tool, onSelect = vm::setTool)
    }

    if (showColorPicker) {
        ColorPickerDialog(
            initial = color,
            onPick = { vm.setColor(it); showColorPicker = false },
            onDismiss = { showColorPicker = false },
        )
    }

    if (showDone) {
        DoneSheet(
            onDismiss = { showDone = false },
            onAction = { action ->
                showDone = false
                when (action) {
                    DoneAction.SAVE -> scope.launch {
                        val saved = withContext(Dispatchers.Default) {
                            val composite = Compositor.render(
                                session.bitmap,
                                session.edits.strokes,
                                session.edits.crop,
                            )
                            MediaStoreSaver.savePng(context, composite)
                        }
                        toast(
                            context,
                            if (saved != null) R.string.saved_to_photos else R.string.capture_failed,
                        )
                        onFinish()
                    }

                    DoneAction.COPY, DoneAction.COPY_DELETE -> scope.launch {
                        val copied = withContext(Dispatchers.Default) {
                            val composite = Compositor.render(
                                session.bitmap,
                                session.edits.strokes,
                                session.edits.crop,
                            )
                            ClipboardCopier.copy(context, composite, session.id)
                        }
                        if (action == DoneAction.COPY_DELETE) {
                            SessionStore.remove(session.id)
                        }
                        toast(context, if (copied) R.string.copied else R.string.capture_failed)
                        onFinish()
                    }

                    DoneAction.DELETE -> {
                        SessionStore.remove(session.id)
                        onFinish()
                    }
                }
            },
        )
    }
}

private fun toast(context: android.content.Context, messageRes: Int) {
    Toast.makeText(context, messageRes, Toast.LENGTH_SHORT).show()
}
