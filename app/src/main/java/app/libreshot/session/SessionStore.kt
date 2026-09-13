package app.libreshot.session

object SessionStore {

    private const val MAX_SESSIONS = 3
    private const val TTL_MS = 10 * 60 * 1000L

    private val sessions = LinkedHashMap<String, ScreenshotSession>()

    @Synchronized
    fun put(session: ScreenshotSession) {
        evictStale()
        sessions[session.id] = session
        while (sessions.size > MAX_SESSIONS) {
            sessions.remove(sessions.keys.first())
        }
    }

    @Synchronized
    fun get(id: String): ScreenshotSession? {
        evictStale()
        return sessions[id]
    }

    @Synchronized
    fun remove(id: String) {
        sessions.remove(id)
    }

    private fun evictStale() {
        val now = System.currentTimeMillis()
        val it = sessions.entries.iterator()
        while (it.hasNext()) {
            if (now - it.next().value.capturedAt > TTL_MS) it.remove()
        }
    }
}
