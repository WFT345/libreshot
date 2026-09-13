package app.libreshot.capture

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import app.libreshot.onboarding.OnboardingActivity
import app.libreshot.session.CaptureSource

class CaptureTile : TileService() {

    companion object {
        fun requestListening(context: Context) {
            requestListeningState(context, ComponentName(context, CaptureTile::class.java))
        }
    }

    override fun onStartListening() {
        qsTile?.apply {
            state = if (CaptureService.instance != null) Tile.STATE_ACTIVE else Tile.STATE_UNAVAILABLE
            updateTile()
        }
    }

    override fun onClick() {
        val target = if (CaptureService.instance != null) {
            // The shade must be gone before capture; CaptureActivity applies the delay.
            Intent(this, CaptureActivity::class.java)
                .putExtra(CaptureActivity.EXTRA_DELAY_MS, 400L)
                .putExtra(CaptureActivity.EXTRA_SOURCE, CaptureSource.TILE.name)
        } else {
            Intent(this, OnboardingActivity::class.java)
        }
        if (Build.VERSION.SDK_INT >= 34) {
            val pending = PendingIntent.getActivity(
                this, 0, target,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            startActivityAndCollapse(pending)
        } else {
            @Suppress("DEPRECATION")
            startActivityAndCollapse(target)
        }
    }
}
