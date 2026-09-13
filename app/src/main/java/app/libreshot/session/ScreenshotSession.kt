package app.libreshot.session

import android.graphics.Bitmap
import app.libreshot.editor.model.EditStack

enum class CaptureSource { QUICK_TAP, TILE, CHORD }

class ScreenshotSession(
    val id: String,
    val bitmap: Bitmap,
    val capturedAt: Long,
    val source: CaptureSource,
) {
    val edits = EditStack()
}
