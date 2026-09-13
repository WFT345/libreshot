package app.libreshot.editor.model

/**
 * Versioned stroke history: every edit pushes a new immutable list, undo/redo move an index.
 * Pure Kotlin so it is unit-testable; the editor mirrors [strokes]/[canUndo]/[canRedo] into state.
 */
class EditStack {

    var strokes: List<Stroke> = emptyList()
        private set
    var canUndo: Boolean = false
        private set
    var canRedo: Boolean = false
        private set

    val hasEdits: Boolean get() = strokes.isNotEmpty()

    private var history = listOf<List<Stroke>>(emptyList())
    private var index = 0

    fun addStroke(stroke: Stroke) = push(strokes + stroke)

    fun removeStroke(stroke: Stroke) {
        val at = strokes.indexOfFirst { it === stroke }
        if (at >= 0) push(strokes.toMutableList().apply { removeAt(at) })
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

    private fun push(next: List<Stroke>) {
        history = history.subList(0, index + 1) + listOf(next)
        index++
        publish()
    }

    private fun publish() {
        strokes = history[index]
        canUndo = index > 0
        canRedo = index < history.size - 1
    }
}
