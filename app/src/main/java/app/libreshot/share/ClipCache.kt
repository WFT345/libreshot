package app.libreshot.share

import android.graphics.Bitmap
import java.io.File
import java.io.IOException

/**
 * The clipboard holds the content URI, not the bytes, so the backing file must survive
 * until the user pastes. Keep the newest [maxFiles] files, prune the rest on each write.
 */
class ClipCache(
    private val dir: File,
    private val maxFiles: Int = 3,
) {

    fun write(id: String, bitmap: Bitmap): File? {
        dir.mkdirs()
        val file = File(dir, "clip_$id.png")
        return try {
            file.outputStream().use { out ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
                    throw IOException("PNG compress failed")
                }
            }
            prune()
            file
        } catch (e: IOException) {
            file.delete()
            null
        }
    }

    fun prune() {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(maxFiles).forEach { it.delete() }
    }
}
