package ax.stardust.plainjane.engine

import ax.stardust.plainjane.config.IOConfig
import java.io.File

/**
 * Responsible for preparing the physical file system environment before the generation pipeline begins.
 *
 * Following the Single Responsibility Principle, this class solely handles the directory
 * scaffolding. It calculates the final destination for the generated models and ensures
 * that the output directory is either freshly cleaned or correctly created before the
 * engines start writing files to disk.
 *
 * @param ioConfig Configuration containing the base output directory and cleanup preferences.
 * @param pkg The target Java package name (e.g., "com.example.api"), used exclusively during
 * initialization to construct the deeply nested folder structure.
 * @property log A logging function to report file system operations back to the user.
 */
class WorkspaceInitializer(
    private val ioConfig: IOConfig,
    pkg: String, // only used upon initialization so no need for private val
    private val log: (String) -> Unit,
) {
    /**
     * The final, resolved directory path where the generated Java files will be placed.
     * This is constructed by appending the converted package structure to the base output directory.
     * For example: `output_dir/com/example/api/`
     */
    val packageDir: File =
        File(
            ioConfig.output,
            pkg.replace('.', File.separatorChar),
        )

    /**
     * Executes the workspace preparation.
     *
     * If the `--clean` flag was specified in the configuration, this method will recursively
     * delete the existing output directory to ensure a pristine generation environment.
     * It then creates all necessary parent directories for the calculated [packageDir].
     */
    fun initialize() {
        // cleaning output directory if desired
        if (ioConfig.clean && ioConfig.output.exists()) {
            log("🧹 Cleaning output directory...")
            ioConfig.output.deleteRecursively()
        }
        packageDir.mkdirs()
    }
}
