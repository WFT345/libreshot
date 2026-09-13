package app.libreshot.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RateLimiterTest {

    @Test
    fun `first call allowed`() {
        assertTrue(RateLimiter(400).allow(10_000))
    }

    @Test
    fun `second call inside window denied`() {
        val limiter = RateLimiter(400)
        limiter.allow(10_000)
        assertFalse(limiter.allow(10_200))
    }

    @Test
    fun `call after window allowed`() {
        val limiter = RateLimiter(400)
        limiter.allow(10_000)
        assertTrue(limiter.allow(10_450))
    }

    @Test
    fun `denied call does not extend the window`() {
        val limiter = RateLimiter(400)
        limiter.allow(10_000)
        limiter.allow(10_200)
        assertTrue(limiter.allow(10_450))
    }
}
