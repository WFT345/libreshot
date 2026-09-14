package app.libreshot.capture

import org.junit.Assert.assertEquals
import org.junit.Test

class LaunchRouterTest {

    @Test
    fun `service off routes to onboarding no matter the caller`() {
        assertEquals(
            LaunchRouter.Action.ONBOARDING,
            LaunchRouter.decide(serviceRunning = false, hasSourceExtra = true, referrerHost = "com.android.systemui"),
        )
        assertEquals(
            LaunchRouter.Action.ONBOARDING,
            LaunchRouter.decide(serviceRunning = false, hasSourceExtra = false, referrerHost = null),
        )
    }

    @Test
    fun `source extra captures regardless of referrer`() {
        assertEquals(
            LaunchRouter.Action.CAPTURE,
            LaunchRouter.decide(serviceRunning = true, hasSourceExtra = true, referrerHost = "app.libreshot"),
        )
        assertEquals(
            LaunchRouter.Action.CAPTURE,
            LaunchRouter.decide(serviceRunning = true, hasSourceExtra = true, referrerHost = null),
        )
    }

    @Test
    fun `systemui launch captures`() {
        assertEquals(
            LaunchRouter.Action.CAPTURE,
            LaunchRouter.decide(serviceRunning = true, hasSourceExtra = false, referrerHost = "com.android.systemui"),
        )
        assertEquals(
            LaunchRouter.Action.CAPTURE,
            LaunchRouter.decide(serviceRunning = true, hasSourceExtra = false, referrerHost = "com.google.android.systemui"),
        )
    }

    @Test
    fun `plain launches open settings`() {
        assertEquals(
            LaunchRouter.Action.OPEN_SETTINGS,
            LaunchRouter.decide(serviceRunning = true, hasSourceExtra = false, referrerHost = "com.android.launcher3"),
        )
        assertEquals(
            LaunchRouter.Action.OPEN_SETTINGS,
            LaunchRouter.decide(serviceRunning = true, hasSourceExtra = false, referrerHost = null),
        )
        assertEquals(
            LaunchRouter.Action.OPEN_SETTINGS,
            LaunchRouter.decide(serviceRunning = true, hasSourceExtra = false, referrerHost = "com.evil.systemui"),
        )
    }
}
