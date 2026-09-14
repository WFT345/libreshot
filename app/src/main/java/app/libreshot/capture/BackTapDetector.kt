package app.libreshot.capture

import kotlin.math.abs

/**
 * Detects a double-tap on the device's back from raw accelerometer samples.
 * Pure logic for unit tests: no Android types, timestamps in ms (monotonic).
 *
 * A tap is a z-dominant high-passed spike (taps hit perpendicular to the screen;
 * the dominance check rejects side knocks and most vibration-motor buzz, which is
 * lateral). Consecutive super-threshold samples within [spikeDeadMs] are one tap.
 * A pair [minGapMs, maxGapMs] apart fires; closer reads as vibration and disarms.
 * After a fire, samples are ignored for [cooldownMs].
 *
 * [onSpike] exists for on-device threshold tuning via logcat; pass null in release.
 */
class BackTapDetector(
    private val threshold: Float = 2.5f,
    private val minGapMs: Long = 80L,
    private val maxGapMs: Long = 400L,
    private val spikeDeadMs: Long = 60L,
    private val cooldownMs: Long = 1000L,
    private val gravityAlpha: Float = 0.1f,
    private val onSpike: ((tMs: Long, hz: Float) -> Unit)? = null,
) {

    private var gx = 0f
    private var gy = 0f
    private var gz = 0f
    private var warmedUp = false
    private var armedAt = 0L
    private var lastSpikeSampleAt = 0L
    private var firedAt = 0L

    /** Returns true when this sample completes the double tap. */
    fun onSample(tMs: Long, x: Float, y: Float, z: Float): Boolean {
        if (!warmedUp) {
            gx = x; gy = y; gz = z
            warmedUp = true
            return false
        }
        gx += gravityAlpha * (x - gx)
        gy += gravityAlpha * (y - gy)
        gz += gravityAlpha * (z - gz)
        val hx = x - gx
        val hy = y - gy
        val hz = z - gz

        if (firedAt != 0L && tMs - firedAt < cooldownMs) return false

        val isSpike = abs(hz) >= threshold && abs(hz) > abs(hx) && abs(hz) > abs(hy)
        if (!isSpike) return false
        if (tMs - lastSpikeSampleAt <= spikeDeadMs) {
            lastSpikeSampleAt = tMs // same physical tap; keep absorbing
            return false
        }
        lastSpikeSampleAt = tMs
        onSpike?.invoke(tMs, hz)

        val gap = tMs - armedAt
        return when {
            armedAt == 0L -> { armedAt = tMs; false }
            gap < minGapMs -> { armedAt = 0L; false }          // vibration, not fingers
            gap <= maxGapMs -> { armedAt = 0L; firedAt = tMs; true }
            else -> { armedAt = tMs; false }                   // stale first tap: re-arm
        }
    }
}
