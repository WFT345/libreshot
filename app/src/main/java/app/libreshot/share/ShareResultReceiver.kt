package app.libreshot.share

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Handler
import java.io.File

/**
 * Fired when the user picks a share target. The temp file is deleted shortly after; the
 * prune-on-write in [ShareHelper] is the backstop if the process dies first.
 */
class ShareResultReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_SHARED = "app.libreshot.action.SHARED"
        const val EXTRA_PATH = "app.libreshot.extra.PATH"
        private const val CLEANUP_DELAY_MS = 60_000L
    }

    override fun onReceive(context: Context, intent: Intent) {
        val path = intent.getStringExtra(EXTRA_PATH) ?: return
        Handler(context.mainLooper).postDelayed(
            { File(path).delete() },
            CLEANUP_DELAY_MS,
        )
    }
}
