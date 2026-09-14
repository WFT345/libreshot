package app.libreshot.capture

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.PowerManager
import android.util.Log
import app.libreshot.BuildConfig

/**
 * Owns the accelerometer subscription for the back-tap trigger. Listens only while
 * [setEnabled] true AND the screen is interactive, so the sensor costs nothing idle.
 * SENSOR_DELAY_FASTEST is silently capped at 200 Hz without HIGH_SAMPLING_RATE_SENSORS,
 * which is enough for 10-30 ms tap transients; callbacks arrive on the registering
 * (main) thread. [onTap] decides what a tap does.
 */
class BackTapMonitor(
    private val context: Context,
    private val onTap: () -> Unit,
) : SensorEventListener {

    private companion object {
        const val TAG = "BackTap"
    }

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val detector = BackTapDetector(
        onSpike = if (BuildConfig.DEBUG) { t, hz -> Log.d(TAG, "spike t=$t hz=$hz") } else null,
    )
    private var enabled = false
    private var listening = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) = updateListening()
    }

    fun setEnabled(value: Boolean) {
        if (enabled == value) return
        enabled = value
        if (value) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            }
            context.registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.unregisterReceiver(screenReceiver)
        }
        updateListening()
    }

    fun destroy() = setEnabled(false)

    private fun updateListening() {
        val want = enabled && accelerometer != null && powerManager?.isInteractive == true
        if (want == listening) return
        listening = want
        if (want) {
            sensorManager?.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        } else {
            sensorManager?.unregisterListener(this)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        val fired = detector.onSample(
            event.timestamp / 1_000_000,
            event.values[0],
            event.values[1],
            event.values[2],
        )
        if (fired) {
            if (BuildConfig.DEBUG) Log.d(TAG, "fire")
            onTap()
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
