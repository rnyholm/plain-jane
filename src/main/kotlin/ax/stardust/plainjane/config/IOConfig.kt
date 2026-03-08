package ax.stardust.plainjane.config

import java.io.File

/**
 * A type-safe wrapper around the input specification URI or local file path.
 *
 * Utilizing an inline value class prevents "Primitive Obsession". It ensures that
 * the compiler strictly enforces the use of an Input object rather than a raw String,
 * while incurring zero runtime allocation overhead.
 *
 * @property uri The raw string representation of the input path (e.g., "openapi.yaml" or "https://.../spec.json").
 */
@JvmInline
value class Input(
    val uri: String,
) {
    /**
     * Dynamically extracts the file name from the [uri].
     * * This getter smoothly handles both Unix-style (`/`) and Windows-style (`\`)
     * path separators to find the actual file name at the end of the string.
     */
    val name: String
        get() = uri.substringAfterLast("/").substringAfterLast("\\")
}

/**
 * Encapsulates the core Input/Output configuration for the generation pipeline.
 *
 * This data class holds the vital paths and directives for where data comes from
 * and where the final artifacts should be written. It bridges the gap between
 * the user's CLI arguments and the internal engines.
 *
 * @property input The source OpenAPI specification.
 * @property output The root destination directory where the generated models and project files will be saved.
 * @property clean If true, instructs the workspace initialization phase to completely wipe the [output] directory before generating new files.
 */
data class IOConfig(
    val input: Input,
    val output: File,
    val clean: Boolean,
)
