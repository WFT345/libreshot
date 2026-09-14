package app.libreshot.editor.model

/**
 * Versioned edit history: every edit pushes a new immutable snapshot, undo/redo move an index.
 * Pure Kotlin so it is unit-testable; the editor mirrors [strokes]/[crop]/[canUndo]/[canRedo]
 * into state.
 */
class EditStack {

    data class Snapshot(
        val strokes: List<Stroke>,
        val crop: CropRect,
    )

    var strokes: List<Stroke> = emptyList()
        private set
    var crop: CropRect = CropRect.FULL
        private set
    var canUndo: Boolean = false
        private set
    var canRedo: Boolean = false
        private set

    val hasEdits: Boolean get() = strokes.isNotEmpty() || !crop.isFull

    private var history = listOf(Snapshot(emptyList(), CropRect.FULL))
    private var index = 0

    fun addStroke(stroke: Stroke) = push(Snapshot(strokes + stroke, crop))

    fun removeStroke(stroke: Stroke) {
        val at = strokes.indexOfFirst { it === stroke }
        if (at >= 0) push(Snapshot(strokes.toMutableList().apply { removeAt(at) }, crop))
    }

    fun setCrop(value: CropRect) {
        if (value != crop) push(Snapshot(strokes, value))
    }

    fun undo() {
        if (!canUndo) return
        index--
        publish()
    }

    fun redo() {
        if (!canRedo) return
        index++
        publish()
    }

    private fun push(next: Snapshot) {
        history = history.subList(0, index + 1) + listOf(next)
        index++
        publish()
    }

    private fun publish() {
        strokes = history[index].strokes
        crop = history[index].crop
        canUndo = index > 0
        canRedo = index < history.size - 1
    }
}
