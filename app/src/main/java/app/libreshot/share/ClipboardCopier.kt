package app.libreshot.share

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.core.content.FileProvider
import app.libreshot.R
import java.io.File

object ClipboardCopier {

    fun copy(context: Context, bitmap: android.graphics.Bitmap, sessionId: String): Boolean {
        val file = ClipCache(File(context.cacheDir, "clip")).write(sessionId, bitmap)
            ?: return false
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file,
        )
        val clipboard = context.getSystemService(ClipboardManager::class.java) ?: return false
        val label = context.getString(R.string.app_name)
        val clip = ClipData.newUri(context.contentResolver, label, uri)
        // Keeps the screenshot out of the system clipboard preview and clipboard history.
        clip.description.extras = (clip.description.extras ?: PersistableBundle()).apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
        clipboard.setPrimaryClip(clip)
        return true
    }
}
