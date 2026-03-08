package ax.stardust.plainjane.cli

import ax.stardust.plainjane.config.ToolVersions
import java.io.InputStream
import java.util.Properties

/**
 * Utility object responsible for loading tool version metadata from the classpath.
 *
 * This loader reads a `version.properties` file generated during the build process
 * (e.g., by Gradle or Maven) to inject the correct versions of Plain Jane and the
 * underlying OpenAPI Generator engine into the application at runtime.
 */
object VersionLoader {
    /**
     * Locates and loads the version properties file from the application's classpath.
     *
     * @return A [ToolVersions] instance containing the resolved version strings,
     * or safe fallback values if the properties file cannot be found.
     */
    fun load(): ToolVersions {
        // use the class loader to load the resource
        val stream = VersionLoader::class.java.getResourceAsStream("/version.properties")
        return loadFromStream(stream)
    }

    /**
     * Parses the version properties from a given input stream.
     *
     * This method is marked as `internal` to expose it for unit testing. By accepting an
     * arbitrary [InputStream], tests can supply in-memory byte arrays to verify parsing logic
     * without relying on physical files or classpath resources.
     *
     * The provided stream is safely and automatically closed after reading.
     *
     * @param stream The [InputStream] containing the properties data, or null if the resource is missing.
     * @return A [ToolVersions] instance populated with the parsed or default fallback versions.
     */
    internal fun loadFromStream(stream: InputStream?): ToolVersions {
        // use the provided input stream to load properties, automatically closes the stream after use
        val props = Properties()
        stream?.use { props.load(it) }

        return ToolVersions(
            plainJaneVersion = props.getProperty("plainjane.version", "0.0.0-DEV"),
            engineVersion = props.getProperty("openapi.generator.version", "unknown"),
        )
    }
}
