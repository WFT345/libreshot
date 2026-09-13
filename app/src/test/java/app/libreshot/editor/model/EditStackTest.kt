package app.libreshot.editor.model

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditStackTest {

    private fun stroke(id: Float) = Stroke(
        points = listOf(Offset(id, id)),
        color = Color.Black,
        widthPx = 4f,
        alpha = 1f,
        tool = Tool.PEN,
    )

    @Test
    fun `add then undo then redo`() {
        val stack = EditStack()
        val a = stroke(1f)
        stack.addStroke(a)

        assertEquals(listOf(a), stack.strokes)
        assertTrue(stack.canUndo)
        assertFalse(stack.canRedo)

        stack.undo()
        assertTrue(stack.strokes.isEmpty())
        assertFalse(stack.canUndo)
        assertTrue(stack.canRedo)

        stack.redo()
        assertEquals(listOf(a), stack.strokes)
    }

    @Test
    fun `new edit after undo clears redo`() {
        val stack = EditStack()
        val a = stroke(1f)
        val b = stroke(2f)
        stack.addStroke(a)
        stack.undo()
        stack.addStroke(b)

        assertFalse(stack.canRedo)
        assertEquals(listOf(b), stack.strokes)
    }

    @Test
    fun `remove stroke undoes back to previous state`() {
        val stack = EditStack()
        val a = stroke(1f)
        val b = stroke(2f)
        stack.addStroke(a)
        stack.addStroke(b)
        stack.removeStroke(a)

        assertEquals(listOf(b), stack.strokes)
        stack.undo()
        assertEquals(listOf(a, b), stack.strokes)
    }

    @Test
    fun `undo at bottom is a no-op`() {
        val stack = EditStack()
        stack.undo()
        assertTrue(stack.strokes.isEmpty())
        assertFalse(stack.canUndo)
    }
}
