package ax.stardust.plainjane.config

/**
 * An immutable container for toolchain version metadata.
 *
 * This configuration holds the resolved version strings for both the Plain Jane CLI
 * and the underlying generation engine. These values are typically loaded at startup
 * (e.g., via [ax.stardust.plainjane.cli.VersionLoader]) and are subsequently used
 * to stamp generated files, such as Java source headers and documentation, ensuring
 * clear provenance and traceability.
 *
 * @property plainJaneVersion The current executing version of the Plain Jane CLI.
 * @property engineVersion The version of the external OpenAPI Generator engine.
 */
data class ToolVersions(
    val plainJaneVersion: String,
    val engineVersion: String,
)
