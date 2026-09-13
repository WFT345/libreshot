package app.libreshot.share

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
        clipboard.setPrimaryClip(ClipData.newUri(context.contentResolver, label, uri))
        return true
    }
}
