package app.libreshot.share

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import app.libreshot.R
import app.libreshot.export.Compositor
import app.libreshot.session.SessionStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Invisible chooser target implementing the "Copy" / "Copy and Delete" rows. */
class CopyActivity : ComponentActivity() {

    companion object {
        const val EXTRA_SESSION_ID = "app.libreshot.extra.SESSION_ID"
        const val EXTRA_DELETE = "app.libreshot.extra.DELETE"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val session = intent.getStringExtra(EXTRA_SESSION_ID)?.let(SessionStore::get)
        if (session == null) {
            Toast.makeText(this, R.string.session_gone, Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val deleteAfter = intent.getBooleanExtra(EXTRA_DELETE, false)
        lifecycleScope.launch {
            val copied = withContext(Dispatchers.Default) {
                val composite = Compositor.render(
                    session.bitmap,
                    session.edits.strokes,
                    session.edits.crop,
                )
                ClipboardCopier.copy(applicationContext, composite, session.id)
            }
            if (copied) {
                if (deleteAfter) SessionStore.remove(session.id)
                Toast.makeText(this@CopyActivity, R.string.copied, Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@CopyActivity, R.string.capture_failed, Toast.LENGTH_SHORT).show()
            }
            finish()
        }
    }
}
