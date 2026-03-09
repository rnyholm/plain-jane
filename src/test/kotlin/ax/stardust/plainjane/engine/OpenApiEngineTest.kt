package ax.stardust.plainjane.engine

import ax.stardust.plainjane.TestData.API_TITLE
import ax.stardust.plainjane.TestData.API_VERSION
import ax.stardust.plainjane.TestData.ARTIFACT_ID
import ax.stardust.plainjane.TestData.OPENAPI_GENERATOR_VERSION
import ax.stardust.plainjane.TestData.PACKAGE_NAME
import ax.stardust.plainjane.TestData.PLAIN_JANE_VERSION
import ax.stardust.plainjane.UI
import ax.stardust.plainjane.config.IOConfig
import ax.stardust.plainjane.config.Input
import ax.stardust.plainjane.config.ModelConfig
import ax.stardust.plainjane.config.RuntimeOptions
import ax.stardust.plainjane.config.ToolVersions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OpenApiEngineTest {
    private lateinit var capturedLogs: MutableList<String>
    private val logger: (String) -> Unit = { capturedLogs.add(it) }

    private lateinit var baseModelConfig: ModelConfig
    private lateinit var baseToolVersions: ToolVersions
    private lateinit var baseRuntimeOptions: RuntimeOptions

    @BeforeEach
    fun setup() {
        capturedLogs = mutableListOf()
        baseModelConfig = ModelConfig(pkg = PACKAGE_NAME)
        baseToolVersions = ToolVersions(plainJaneVersion = PLAIN_JANE_VERSION, engineVersion = OPENAPI_GENERATOR_VERSION)
        baseRuntimeOptions = RuntimeOptions(debug = false) // to avoid spamming terminal with debug logging during tests
    }

    // create a minimal valid OpenAPI spec file for testing purposes
    private fun createValidSpec(tempDir: File): File {
        val specFile = File(tempDir, "spec.yaml")
        specFile.writeText(
            """
            openapi: 3.0.0
            info:
              title: $API_TITLE
              version: $API_VERSION
            paths: {}
            components:
              schemas:
                TestModel:
                  type: object
                  properties:
                    id:
                      type: string
            """.trimIndent(),
        )
        return specFile
    }

    @Test
    fun `should parse spec, extract metadata and generate java files`(
        @TempDir tempDir: File,
    ) {
        val specFile = createValidSpec(tempDir)
        val outputDir = File(tempDir, "output")
        val ioConfig = IOConfig(input = Input(specFile.absolutePath), output = outputDir, clean = false)

        val engine = OpenApiEngine(ioConfig, baseModelConfig, baseRuntimeOptions, baseToolVersions, ARTIFACT_ID, logger)

        // verify initial state before generation - metadata should be default values and no files should be generated
        assertEquals("Unknown API", engine.apiTitle)
        assertEquals("Unknown Version", engine.apiVersion)
        assertTrue(engine.generatedJavaFiles.isEmpty())

        engine.generateRawModels()

        // verify metadata extraction
        assertEquals(API_TITLE, engine.apiTitle)
        assertEquals(API_VERSION, engine.apiVersion)

        // verify that the file has been created internally in the engines list of generated files
        assertTrue(engine.generatedJavaFiles.isNotEmpty(), "Engine should have generated java files")
        val isTestModelGenerated = engine.generatedJavaFiles.any { it.name == "TestModel.java" }
        assertTrue(isTestModelGenerated, "TestModel.java should have been generated (exist in the generated java files list)")

        // verify that the generated file physically exists on disk in the expected location
        // note! this location is the "raw" location right after generation and not it's final location after we
        // move it to the package structure, which is done by the orchestrator
        val packagePath = PACKAGE_NAME.replace('.', '/')
        val expectedModelPath = File(outputDir, "src/main/java/$packagePath/TestModel.java")
        assertTrue(expectedModelPath.exists(), "The Java file must physically exist on disk")

        assertContains(capturedLogs.first(), "Starting generation for $ARTIFACT_ID")
        assertContains(capturedLogs.last(), "Raw generation completed")
    }

    @Test
    fun `should throw IllegalArgumentException on invalid OpenAPI spec`(
        @TempDir tempDir: File,
    ) {
        val invalidSpec = File(tempDir, "broken.yaml")
        invalidSpec.writeText("This is not a yaml file. It's just garbage.")

        val ioConfig = IOConfig(input = Input(invalidSpec.absolutePath), output = tempDir, clean = false)
        val engine = OpenApiEngine(ioConfig, baseModelConfig, baseRuntimeOptions, baseToolVersions, ARTIFACT_ID, logger)

        val exception =
            assertThrows<IllegalArgumentException> {
                engine.generateRawModels()
            }

        // !! = assert message is not null
        assertContains(exception.message!!, "Failed to parse OpenAPI specification")
        assertTrue(capturedLogs.any { it.contains("OpenAPI Specification issues found") })
    }

    @Test
    fun `should remove generator bloat from output directory`(
        @TempDir tempDir: File,
    ) {
        val outputDir = File(tempDir, "output").apply { mkdirs() }

        // create fake bloat that OpenAPI Generator typically leaves behind
        val srcDir = File(outputDir, "src").apply { mkdirs() }
        File(srcDir, "SomeCode.java").createNewFile()
        File(outputDir, ".github").apply { mkdirs() }
        File(outputDir, "gradlew").createNewFile()
        File(outputDir, ".openapi-generator").apply { mkdirs() }

        // create a file that should NOT be deleted (e.g. our actual model)
        val validFile = File(outputDir, "ValidFile.txt").apply { createNewFile() }

        val ioConfig = IOConfig(input = Input("dummy"), output = outputDir, clean = false)
        val engine = OpenApiEngine(ioConfig, baseModelConfig, baseRuntimeOptions, baseToolVersions, ARTIFACT_ID, logger)

        engine.removeBloat()

        // verify that the typical bloat files/directories have been deleted and that the valid file is still there
        assertFalse(srcDir.exists(), "'src' directory should be deleted")
        assertFalse(File(outputDir, ".github").exists(), "'.github' directory should be deleted")
        assertFalse(File(outputDir, "gradlew").exists(), "'gradlew' file should be deleted")
        assertFalse(File(outputDir, ".openapi-generator").exists(), "'.openapi-generator' should be deleted")
        assertTrue(validFile.exists(), "Files not in the bloat list should be preserved")

        assertTrue(capturedLogs.any { it.contains("Removing bloat generated by OpenAPI Generator") })
    }

    @Test
    fun `should enable debug logging when runtime option is set to true`(
        @TempDir tempDir: File,
    ) {
        // to test 'configureLogging' block in the engine
        val debugOptions = RuntimeOptions(debug = true)
        val ioConfig = IOConfig(input = Input("dummy"), output = tempDir, clean = false)

        // instation of the engine will trigger configureLogging()
        OpenApiEngine(ioConfig, baseModelConfig, debugOptions, baseToolVersions, ARTIFACT_ID, logger)

        assertTrue(capturedLogs.any { it.contains("${UI.BUG} Debug mode enabled") })
        // note! Since we cannot (or should not) reliably inspect Java's global log levels
        // in a unit test without risking side effects for other tests, we settle for
        // verifying that the branch was executed via the log output.
    }
}
