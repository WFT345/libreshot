package app.libreshot.editor

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import app.libreshot.R
import app.libreshot.session.SessionStore
import app.libreshot.ui.theme.LibreShotTheme

class EditorActivity : ComponentActivity() {

    companion object {
        private const val EXTRA_SESSION_ID = "app.libreshot.extra.SESSION_ID"

        fun intent(context: Context, sessionId: String): Intent =
            Intent(context, EditorActivity::class.java).putExtra(EXTRA_SESSION_ID, sessionId)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This window is the captured screenshot; the system would persist it as a Recents thumbnail.
        setRecentsScreenshotEnabled(false)
        val session = intent.getStringExtra(EXTRA_SESSION_ID)?.let(SessionStore::get)
        if (session == null) {
            Toast.makeText(this, R.string.session_gone, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        enableEdgeToEdge()
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        setContent {
            LibreShotTheme {
                EditorScreen(session = session, onFinish = { finish() })
            }
        }
    }
}
