package app.libreshot

import android.app.Application
import java.io.File

class LibreShotApp : Application() {

    override fun onCreate() {
        super.onCreate()
        File(cacheDir, "share").listFiles()?.forEach { it.delete() }
    }
}
