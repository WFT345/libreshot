package app.libreshot.share

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.os.Handler
import androidx.core.content.FileProvider
import app.libreshot.export.Compositor
import app.libreshot.session.ScreenshotSession
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.concurrent.thread

object ShareHelper {

    private const val MAX_PNG_BYTES = 8 * 1024 * 1024

    /**
     * Composites (when [edited]) and opens the system share sheet. Our own Copy / Copy and
     * Delete actions are pinned at the top like iOS does.
     */
    fun share(context: Context, session: ScreenshotSession, edited: Boolean) {
        val appContext = context.applicationContext
        thread(name = "share-export") {
            val bitmap =
                if (edited) Compositor.render(session.bitmap, session.edits.strokes, session.edits.crop)
                else session.bitmap
            val dir = File(appContext.cacheDir, "share").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }

            val png = ByteArrayOutputStream().also {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
            val file: File
            val mime: String
            if (png.size() > MAX_PNG_BYTES) {
                file = File(dir, "share_${session.id}.jpg")
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 92, it) }
                mime = "image/jpeg"
            } else {
                file = File(dir, "share_${session.id}.png")
                file.writeBytes(png.toByteArray())
                mime = "image/png"
            }

            val uri = FileProvider.getUriForFile(
                appContext,
                "${appContext.packageName}.fileprovider",
                file,
            )
            val chosen = android.app.PendingIntent.getBroadcast(
                appContext,
                session.id.hashCode(),
                Intent(appContext, ShareResultReceiver::class.java)
                    .setAction(ShareResultReceiver.ACTION_SHARED)
                    .putExtra(ShareResultReceiver.EXTRA_PATH, file.absolutePath),
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                    android.app.PendingIntent.FLAG_IMMUTABLE,
            )
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val copy = Intent()
                .setComponent(ComponentName(appContext.packageName, "app.libreshot.share.CopyTarget"))
                .putExtra(CopyActivity.EXTRA_SESSION_ID, session.id)
            val copyDelete = Intent()
                .setComponent(
                    ComponentName(appContext.packageName, "app.libreshot.share.CopyDeleteTarget"),
                )
                .putExtra(CopyActivity.EXTRA_SESSION_ID, session.id)
                .putExtra(CopyActivity.EXTRA_DELETE, true)
            val chooser = Intent.createChooser(send, null).apply {
                putExtra(Intent.EXTRA_CHOSEN_COMPONENT_INTENT_SENDER, chosen.intentSender)
                putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(copy, copyDelete))
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            Handler(appContext.mainLooper).post { appContext.startActivity(chooser) }
        }
    }
}
