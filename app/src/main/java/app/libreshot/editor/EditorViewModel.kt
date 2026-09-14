package app.libreshot.editor

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.libreshot.editor.model.CropHandle
import app.libreshot.editor.model.CropRect
import app.libreshot.editor.model.Stroke
import app.libreshot.editor.model.Tool
import app.libreshot.editor.model.hitTestStroke
import app.libreshot.session.ScreenshotSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class EditorViewModel(private val session: ScreenshotSession) : ViewModel() {

    companion object {
        fun factory(session: ScreenshotSession) = viewModelFactory {
            initializer { EditorViewModel(session) }
        }

        private fun alphaFor(tool: Tool): Float = when (tool) {
            Tool.MARKER -> 0.45f
            Tool.PENCIL -> 0.9f
            else -> 1f
        }
    }

    private val edits get() = session.edits

    private val _strokes = MutableStateFlow(edits.strokes)
    val strokes: StateFlow<List<Stroke>> = _strokes

    private val _liveStroke = MutableStateFlow<Stroke?>(null)
    val liveStroke: StateFlow<Stroke?> = _liveStroke

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo
    private val _hasEdits = MutableStateFlow(false)
    val hasEdits: StateFlow<Boolean> = _hasEdits

    private val _crop = MutableStateFlow(edits.crop)
    val crop: StateFlow<CropRect> = _crop

    val tool = MutableStateFlow(Tool.PEN)
    val color = MutableStateFlow(Color.Black)

    private var activePoints: MutableList<Offset>? = null
    private var activeWidthPx = 0f
    private var cropDragStart: CropRect? = null

    fun setTool(value: Tool) {
        tool.value = value
    }

    fun setColor(value: Color) {
        color.value = value
    }

    fun beginStroke(at: Offset, widthPx: Float) {
        activePoints = mutableListOf(at)
        activeWidthPx = widthPx
        _liveStroke.value = buildStroke(listOf(at))
    }

    fun extendStroke(at: Offset) {
        val points = activePoints ?: return
        val last = points.last()
        val dx = at.x - last.x
        val dy = at.y - last.y
        if (dx * dx + dy * dy < 1f) return
        points.add(at)
        _liveStroke.value = buildStroke(points.toList())
    }

    fun endStroke() {
        val points = activePoints ?: return
        activePoints = null
        _liveStroke.value = null
        edits.addStroke(buildStroke(points.toList()))
        sync()
    }

    fun cancelStroke() {
        activePoints = null
        _liveStroke.value = null
    }

    fun eraseAt(at: Offset, tolerancePx: Float) {
        val hit = hitTestStroke(edits.strokes, at, tolerancePx) ?: return
        edits.removeStroke(hit)
        sync()
    }

    /** Starts a crop-handle drag; moves are absolute against the pre-drag rect. */
    fun beginCropDrag() {
        cropDragStart = _crop.value
    }

    fun dragCrop(handle: CropHandle, toNormalized: Offset) {
        val start = cropDragStart ?: return
        _crop.value = start.dragged(handle, toNormalized.x, toNormalized.y)
    }

    fun endCropDrag() {
        val start = cropDragStart ?: return
        cropDragStart = null
        if (_crop.value != start) {
            edits.setCrop(_crop.value)
            sync()
        }
    }

    fun cancelCropDrag() {
        cropDragStart?.let { _crop.value = it }
        cropDragStart = null
    }

    fun undo() {
        edits.undo()
        sync()
    }

    fun redo() {
        edits.redo()
        sync()
    }

    private fun buildStroke(points: List<Offset>) = Stroke(
        points = points,
        color = color.value,
        widthPx = activeWidthPx,
        alpha = alphaFor(tool.value),
        tool = tool.value,
    )

    private fun sync() {
        _strokes.value = edits.strokes
        _crop.value = edits.crop
        _canUndo.value = edits.canUndo
        _canRedo.value = edits.canRedo
        _hasEdits.value = edits.hasEdits
    }
}
