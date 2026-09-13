package app.libreshot.share

import java.io.File
import org.junit.Assert.assertEquals
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
    fun `prune on empty dir is a no-op`() {
        val dir = tmp.newFolder("empty")
        ClipCache(dir).prune()
        assertTrue(dir.listFiles().isNullOrEmpty())
    }
}
