package ax.stardust.plainjane.cli

import ax.stardust.plainjane.TestData.FALLBACK_OPEN_API_GENERATOR_VERSION
import ax.stardust.plainjane.TestData.FALLBACK_PLAIN_JANE_VERSION
import ax.stardust.plainjane.TestData.OPENAPI_GENERATOR_VERSION
import ax.stardust.plainjane.TestData.PLAIN_JANE_VERSION
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertEquals

class VersionLoaderTest {
    @Test
    fun `should load correct versions from valid properties stream`() {
        // create faked properties content with both keys and open the stream
        val propertiesContent =
            """
            plainjane.version=$PLAIN_JANE_VERSION
            openapi.generator.version=$OPENAPI_GENERATOR_VERSION
            """.trimIndent()
        val stream = propertiesContent.byteInputStream()

        // inject the stream directly to bypass classpath loading
        val result = VersionLoader.loadFromStream(stream)
        assertEquals(PLAIN_JANE_VERSION, result.plainJaneVersion)
        assertEquals(OPENAPI_GENERATOR_VERSION, result.engineVersion)
    }

    @Test
    fun `should use fallback values when stream is null (file not found)`() {
        // inject null, which is exactly what getResourceAsStream returns if the file is missing
        val result = VersionLoader.loadFromStream(null)
        assertEquals(FALLBACK_PLAIN_JANE_VERSION, result.plainJaneVersion)
        assertEquals(FALLBACK_OPEN_API_GENERATOR_VERSION, result.engineVersion)
    }

    @Test
    fun `should use fallback values for missing properties in stream`() {
        // creates properties content with only plainjane.version, missing openapi.generator.version
        val propertiesContent = "plainjane.version=$PLAIN_JANE_VERSION"
        val stream = propertiesContent.byteInputStream()

        val result = VersionLoader.loadFromStream(stream)
        assertEquals(PLAIN_JANE_VERSION, result.plainJaneVersion)
        assertEquals(FALLBACK_OPEN_API_GENERATOR_VERSION, result.engineVersion)
    }

    @Test
    fun `should use fallback values when stream is empty`() {
        val stream = ByteArrayInputStream(ByteArray(0))
        val result = VersionLoader.loadFromStream(stream)

        assertEquals(FALLBACK_PLAIN_JANE_VERSION, result.plainJaneVersion)
        assertEquals(FALLBACK_OPEN_API_GENERATOR_VERSION, result.engineVersion)
    }

    @Test
    fun `should handle malformed properties content gracefully`() {
        val propertiesContent = "THIS IS JUST GARBAGE TEXT NO EQUALS SIGN"
        val stream = propertiesContent.byteInputStream()

        val result = VersionLoader.loadFromStream(stream)

        assertEquals(FALLBACK_PLAIN_JANE_VERSION, result.plainJaneVersion)
        assertEquals(FALLBACK_OPEN_API_GENERATOR_VERSION, result.engineVersion)
    }
}
