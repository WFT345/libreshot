package app.libreshot.share

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ClipCacheTest {

    @get:Rule
    val tmp = TemporaryFolder()

    @Test
    fun `prune keeps the newest files`() {
        val dir = tmp.newFolder("clip")
        val cache = ClipCache(dir, maxFiles = 3)
        val files = (1..5).map { i ->
            File(dir, "clip_$i.png").apply {
                writeBytes(byteArrayOf(i.toByte()))
                setLastModified(1_000_000L + i * 1000L)
            }
        }

        cache.prune()

        val remaining = dir.listFiles()?.map { it.name }?.sorted() ?: emptyList()
        assertEquals(listOf("clip_3.png", "clip_4.png", "clip_5.png"), remaining)
        assertTrue(files[4].exists())
    }

    @Test
    fun `sweep deletes only files past the ttl`() {
        val dir = tmp.newFolder("clip")
        val stale = File(dir, "clip_old.png").apply {
            writeBytes(byteArrayOf(1))
            setLastModified(1_000_000L)
        }
        val fresh = File(dir, "clip_new.png").apply {
            writeBytes(byteArrayOf(2))
            setLastModified(1_600_000L)
        }

        ClipCache(dir).sweepOlderThan(ttlMs = 500_000L, nowMs = 2_000_000L)

        assertFalse(stale.exists())
        assertTrue(fresh.exists())
    }

    @Test
    fun `prune on empty dir is a no-op`() {
        val dir = tmp.newFolder("empty")
        ClipCache(dir).prune()
        assertTrue(dir.listFiles().isNullOrEmpty())
    }
}
