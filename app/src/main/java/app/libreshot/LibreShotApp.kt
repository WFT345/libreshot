package app.libreshot

import android.app.Application
import app.libreshot.share.ClipCache
import java.io.File

class LibreShotApp : Application() {

    companion object {
        // Clip files outlive the process on purpose (the clipboard holds a URI into them), so they
        // can only be swept by age, not unconditionally.
        private const val CLIP_TTL_MS = 60 * 60 * 1000L
    }

    override fun onCreate() {
        super.onCreate()
        File(cacheDir, "share").listFiles()?.forEach { it.delete() }
        ClipCache(File(cacheDir, "clip")).sweepOlderThan(CLIP_TTL_MS)
    }
}
