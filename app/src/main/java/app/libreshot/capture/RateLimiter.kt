package app.libreshot.capture

/** Trailing-edge guard: at most one capture per [windowMs]. */
class RateLimiter(private val windowMs: Long) {

    private var lastAllowedAt = 0L

    fun allow(nowMs: Long): Boolean {
        if (nowMs - lastAllowedAt < windowMs) return false
        lastAllowedAt = nowMs
        return true
    }
}
