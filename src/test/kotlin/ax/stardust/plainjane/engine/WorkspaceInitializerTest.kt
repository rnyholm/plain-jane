package ax.stardust.plainjane.engine

import ax.stardust.plainjane.TestData.PACKAGE_NAME
import ax.stardust.plainjane.TestData.PETSTORE_URL
import ax.stardust.plainjane.config.IOConfig
import ax.stardust.plainjane.config.Input
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WorkspaceInitializerTest {
    private lateinit var capturedLogs: MutableList<String>
    private val logger: (String) -> Unit = { capturedLogs.add(it) }

    // we need a "dummy" input to satisfy the IOConfig constructor
    private val dummyInput = Input(uri = PETSTORE_URL)

    @BeforeEach
    fun setup() {
        capturedLogs = mutableListOf()
    }

    @Test
    fun `should calculate correct package directory path`(
        @TempDir tempDir: File,
    ) {
        val outputDir = File(tempDir, "output")
        val config = IOConfig(input = dummyInput, output = outputDir, clean = false)
        val initializer = WorkspaceInitializer(ioConfig = config, pkg = PACKAGE_NAME, log = logger)

        // expected path should be outputDir + package structure (with dots replaced by file separators)
        val expectedPath =
            File(outputDir, PACKAGE_NAME.replace('.', File.separatorChar)).absolutePath
        assertEquals(expectedPath, initializer.packageDir.absolutePath)
    }

    @Test
    fun `should handle empty root package correctly`(
        @TempDir tempDir: File,
    ) {
        val outputDir = File(tempDir, "output")
        val config = IOConfig(input = dummyInput, output = outputDir, clean = false)

        // edge-case, user provides an empty string as the package name
        // expected path should then just be the output directory itself, without any subfolders
        val initializer = WorkspaceInitializer(ioConfig = config, pkg = "", log = logger)
        assertEquals(outputDir.absolutePath, initializer.packageDir.absolutePath)
    }

    @Test
    fun `should create directories without deleting existing files when clean is false`(
        @TempDir tempDir: File,
    ) {
        // build up a structure of existing files that should be preserved
        val outputDir = File(tempDir, "output").apply { mkdirs() }
        val existingFile = File(outputDir, "keep-me.txt").apply { createNewFile() }

        val config = IOConfig(input = dummyInput, output = outputDir, clean = false)
        val initializer = WorkspaceInitializer(ioConfig = config, pkg = PACKAGE_NAME, log = logger)
        initializer.initialize()

        // verify that the existing files are still there and that the new package directories are created
        assertTrue(existingFile.exists(), "Existing file should NOT be deleted when clean=false")
        assertTrue(
            initializer.packageDir.exists() && initializer.packageDir.isDirectory,
            "Package directory should be created",
        )
        assertTrue(capturedLogs.isEmpty(), "Should not log cleaning message")
    }

    @Test
    fun `should recursively delete output directory and recreate when clean is true`(
        @TempDir tempDir: File,
    ) {
        // build up a structure of existing files that should be deleted
        val outputDir = File(tempDir, "output").apply { mkdirs() }
        val oldFile = File(outputDir, "delete-me.txt").apply { createNewFile() }
        val oldSubDir = File(outputDir, "old-dir").apply { mkdirs() }
        val oldSubFile = File(oldSubDir, "delete-me-too.txt").apply { createNewFile() }

        val config = IOConfig(input = dummyInput, output = outputDir, clean = true)
        val initializer = WorkspaceInitializer(ioConfig = config, pkg = PACKAGE_NAME, log = logger)
        initializer.initialize()

        // verify that the old files and directories have been deleted
        assertFalse(oldFile.exists(), "Old root file should be deleted")
        assertFalse(
            oldSubDir.exists() || oldSubFile.exists(),
            "Old subdirectories and files should be deleted",
        )

        // verify that the new package directory is created
        assertTrue(
            initializer.packageDir.exists() && initializer.packageDir.isDirectory,
            "New package directory should be created",
        )

        // verify logging of cleaning action
        assertEquals(1, capturedLogs.size)
        assertContains(capturedLogs.first(), "Cleaning output directory")
    }

    @Test
    fun `should not crash when clean is true but output directory does not exist yet`(
        @TempDir tempDir: File,
    ) {
        val nonExistentOutputDir = File(tempDir, "new-output")

        // sanity check to ensure the directory really doesn't exist before we run the initializer
        assertFalse(nonExistentOutputDir.exists())

        val config = IOConfig(input = dummyInput, output = nonExistentOutputDir, clean = true)
        val initializer = WorkspaceInitializer(ioConfig = config, pkg = PACKAGE_NAME, log = logger)
        initializer.initialize()

        // verify that the package directory is created successfully even though the base output directory didn't exist before
        assertTrue(
            initializer.packageDir.exists() && initializer.packageDir.isDirectory,
            "Package directory should be created",
        )
        assertTrue(
            capturedLogs.isEmpty(),
            "Should not log cleaning message if directory didn't exist in the first place",
        )
    }
}
