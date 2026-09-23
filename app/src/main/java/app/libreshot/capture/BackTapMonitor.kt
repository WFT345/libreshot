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
import app.libreshot.settings.PrefsSnapshot

/**
 * Owns the accelerometer subscription for the back-tap trigger. Listens only while
 * [setEnabled] true AND the screen is interactive, so the sensor costs nothing idle.
 * Samples at 200 Hz, the fastest rate allowed without HIGH_SAMPLING_RATE_SENSORS, which
 * is enough for 10 to 30 ms tap transients; callbacks arrive on the registering
 * (main) thread. [onTap] decides what a tap does.
 */
class BackTapMonitor(
    private val context: Context,
    private val onTap: () -> Unit,
) : SensorEventListener {

    private companion object {
        const val TAG = "BackTap"

        /** 200 Hz. Below this (SENSOR_DELAY_FASTEST is 0) the platform demands
         *  HIGH_SAMPLING_RATE_SENSORS, and this app declares no permissions. */
        const val SAMPLING_PERIOD_US = 5_000
    }

    private val sensorManager = context.getSystemService(SensorManager::class.java)
    private val powerManager = context.getSystemService(PowerManager::class.java)
    private val accelerometer: Sensor? = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var threshold = PrefsSnapshot().backTapThreshold
    private var detector = newDetector(threshold)
    private var enabled = false
    private var listening = false

    /** Swapping the detector restarts its gravity estimate, which re-settles in a sample. */
    fun setThreshold(value: Float) {
        if (value == threshold) return
        threshold = value
        detector = newDetector(value)
    }

    private fun newDetector(value: Float) = BackTapDetector(
        threshold = value,
        onSpike = if (BuildConfig.DEBUG) { t, hz -> Log.d(TAG, "spike t=$t hz=$hz") } else null,
    )

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
            try {
                context.unregisterReceiver(screenReceiver)
            } catch (e: IllegalArgumentException) {
            }
        }
        updateListening()
    }

    fun destroy() = setEnabled(false)

    private fun updateListening() {
        val want = enabled && accelerometer != null && powerManager?.isInteractive == true
        if (want == listening) return
        listening = want
        if (want) {
            listening = register(SAMPLING_PERIOD_US) || register(SensorManager.SENSOR_DELAY_GAME)
        } else {
            try {
                sensorManager?.unregisterListener(this)
            } catch (e: RuntimeException) {
            }
        }
    }

    /**
     * Anything faster than [SAMPLING_PERIOD_US] throws without HIGH_SAMPLING_RATE_SENSORS,
     * and an uncaught throw here disables the whole accessibility service, so failures
     * fall back to a slower rate and finally to the feature simply not running.
     */
    private fun register(periodUs: Int): Boolean = try {
        sensorManager?.registerListener(this, accelerometer, periodUs) == true
    } catch (e: RuntimeException) {
        Log.w(TAG, "accelerometer registration failed at ${periodUs}us", e)
        false
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
