package ax.stardust.plainjane

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class UITest {
    private val isWindowsOS = System.getProperty("os.name").contains("windows", ignoreCase = true)
    private val forceEmojis = System.getenv("FORCE_EMOJIS") == "true"
    private val expectingAscii = isWindowsOS && !forceEmojis

    @Test
    fun `UI markers should match the expected platform format`() {
        if (expectingAscii) {
            // Windows environment -> expect ASCII markers, which are padded for alignment
            assertEquals("==>  ", UI.ROCKET)
            assertEquals("  v  ", UI.CHECK)
            assertEquals("  ~  ", UI.SWEEP)
            assertEquals("  ~  ", UI.SOAP)
            assertEquals("  -  ", UI.TRASH)
            assertEquals("  +  ", UI.PACKAGE)
            assertEquals("  +  ", UI.DOC)
            assertEquals("  >  ", UI.MONKEY)
            assertEquals("***  ", UI.STARS)
            assertEquals("  !  ", UI.BUG)
            assertEquals("  !  ", UI.WARNING)
            assertEquals("  x  ", UI.ERROR)
        } else {
            // Linux/Mac environment -> expect Emojis
            assertEquals("🚀", UI.ROCKET)
            assertEquals("✅", UI.CHECK)
            assertEquals("🧹", UI.SWEEP)
            assertEquals("🧼", UI.SOAP)
            assertEquals("🗑️", UI.TRASH)
            assertEquals("📦", UI.PACKAGE)
            assertEquals("📄", UI.DOC)
            assertEquals("🙈", UI.MONKEY)
            assertEquals("✨", UI.STARS)
            assertEquals("🐞", UI.BUG)
            assertEquals("⚠️", UI.WARNING)
            assertEquals("❌", UI.ERROR)
        }
    }

    @Test
    fun `ASCII markers should be consistently padded for vertical alignment`() {
        if (expectingAscii) {
            val allMarkers =
                listOf(
                    UI.ROCKET,
                    UI.CHECK,
                    UI.SWEEP,
                    UI.SOAP,
                    UI.TRASH,
                    UI.PACKAGE,
                    UI.DOC,
                    UI.MONKEY,
                    UI.STARS,
                    UI.BUG,
                    UI.WARNING,
                    UI.ERROR,
                )

            allMarkers.forEach { marker ->
                assertEquals(5, marker.length, "Marker '$marker' breaks the alignment!")
            }
        }
    }
}
