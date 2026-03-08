package ax.stardust.plainjane

import ax.stardust.plainjane.engine.ExtrasGenerator
import ax.stardust.plainjane.engine.HeaderInjector
import ax.stardust.plainjane.engine.JavaSanitizer
import ax.stardust.plainjane.engine.OpenApiEngine
import ax.stardust.plainjane.engine.WorkspaceInitializer
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ModelOrchestratorTest {
    private lateinit var workspaceMock: WorkspaceInitializer
    private lateinit var engineMock: OpenApiEngine
    private lateinit var sanitizerMock: JavaSanitizer
    private lateinit var headerInjectorMock: HeaderInjector
    private lateinit var extrasGeneratorMock: ExtrasGenerator

    private lateinit var capturedLogs: MutableList<String>
    private val logger: (String) -> Unit = { capturedLogs.add(it) }

    @BeforeEach
    fun setup() {
        workspaceMock = mockk()
        engineMock = mockk()
        sanitizerMock = mockk()
        headerInjectorMock = mockk()
        extrasGeneratorMock = mockk()
        capturedLogs = mutableListOf()

        every { workspaceMock.initialize() } just runs
        every { engineMock.generateRawModels() } just runs
        every { engineMock.removeBloat() } just runs
        every { engineMock.apiTitle } returns TestData.API_TITLE
        every { engineMock.apiVersion } returns TestData.API_VERSION
        every { extrasGeneratorMock.generate(any(), any()) } just runs
    }

    @Test
    fun `should successfully process all files and return complete result`(
        @TempDir tempDir: File,
    ) {
        val sourceDir = File(tempDir, "source").apply { mkdirs() }
        val destDir = File(tempDir, "destination").apply { mkdirs() }

        val file1 = File(sourceDir, "ModelOne.java").apply { writeText("raw code 1") }
        val file2 = File(sourceDir, "ModelTwo.java").apply { writeText("raw code 2") }

        every { workspaceMock.packageDir } returns destDir
        every { engineMock.generatedJavaFiles } returns listOf(file1, file2)

        // simulate that both files were sanitized successfully
        every { sanitizerMock.sanitize(file1) } returns JavaSanitizer.Result(code = "clean code 1", isSanitized = true)
        every { sanitizerMock.sanitize(file2) } returns JavaSanitizer.Result(code = "clean code 2", isSanitized = true)

        // simulate header injection for both files
        every { headerInjectorMock.inject(code = "clean code 1", apiTitle = any(), apiVersion = any()) } returns "HEADER clean code 1"
        every { headerInjectorMock.inject(code = "clean code 2", apiTitle = any(), apiVersion = any()) } returns "HEADER clean code 2"

        val orchestrator =
            ModelOrchestrator(
                workspaceMock,
                engineMock,
                sanitizerMock,
                headerInjectorMock,
                extrasGeneratorMock,
                logger,
            )

        val result = orchestrator.orchestrate()

        // verify that 2 of 2 files were sanitized successfully and none failed
        assertEquals(2, result.totalCount)
        assertEquals(2, result.sanitizedCount)
        assertEquals(0, result.failedCount)

        // verify that the files were written to the correct destination with correct content
        val destFile1 = File(destDir, "ModelOne.java")
        val destFile2 = File(destDir, "ModelTwo.java")
        assertTrue(destFile1.exists())
        assertEquals("HEADER clean code 1", destFile1.readText())
        assertEquals("HEADER clean code 2", destFile2.readText())

        // verify that the dependencies of the orchestrator were called correctly
        verify(exactly = 1) { workspaceMock.initialize() }
        verify(exactly = 1) { engineMock.generateRawModels() }
        verify(exactly = 1) { engineMock.removeBloat() }
        verify(exactly = 1) { extrasGeneratorMock.generate(TestData.API_TITLE, TestData.API_VERSION) }
    }

    @Test
    fun `should skip header injection for files that failed sanitization and update counters correctly`(
        @TempDir tempDir: File,
    ) {
        val sourceDir = File(tempDir, "source").apply { mkdirs() }
        val destDir = File(tempDir, "destination").apply { mkdirs() }

        val validFile = File(sourceDir, "Valid.java").apply { writeText("raw valid") }
        val brokenFile = File(sourceDir, "Broken.java").apply { writeText("raw broken") }

        every { workspaceMock.packageDir } returns destDir
        every { engineMock.generatedJavaFiles } returns listOf(validFile, brokenFile)

        // simulate 1 successful and 1 failed sanitization
        every { sanitizerMock.sanitize(validFile) } returns JavaSanitizer.Result("clean valid", isSanitized = true)
        every { sanitizerMock.sanitize(brokenFile) } returns JavaSanitizer.Result("raw broken", isSanitized = false)

        every { headerInjectorMock.inject("clean valid", any(), any()) } returns "HEADER clean valid"
        // no mock for headerInjector on brokenFile since it should not be called

        val orchestrator =
            ModelOrchestrator(
                workspaceMock,
                engineMock,
                sanitizerMock,
                headerInjectorMock,
                extrasGeneratorMock,
                logger,
            )

        val result = orchestrator.orchestrate()

        // verify 1 failed and 1 sanitized file
        assertEquals(2, result.totalCount)
        assertEquals(1, result.sanitizedCount)
        assertEquals(1, result.failedCount)

        // verify that the broken file was written with its original unsanitized code and without header injection
        assertEquals("HEADER clean valid", File(destDir, "Valid.java").readText())
        assertEquals("raw broken", File(destDir, "Broken.java").readText())

        // verify that header injection was only called once (for the successfully sanitized file)
        verify(exactly = 1) { headerInjectorMock.inject(any(), any(), any()) }
    }

    @Test
    fun `should handle empty file list gracefully`(
        @TempDir tempDir: File,
    ) {
        val destDir = File(tempDir, "destination").apply { mkdirs() }
        every { workspaceMock.packageDir } returns destDir
        every { engineMock.generatedJavaFiles } returns emptyList() // no files were generated by the engine

        val orchestrator =
            ModelOrchestrator(
                workspaceMock,
                engineMock,
                sanitizerMock,
                headerInjectorMock,
                extrasGeneratorMock,
                logger,
            )

        val result = orchestrator.orchestrate()

        // verify that every field should be 0 since there were no files to process
        assertEquals(0, result.totalCount)
        assertEquals(0, result.sanitizedCount)
        assertEquals(0, result.failedCount)

        // verify that neither sanitizer nor header injector were called since there were no files to process
        verify(exactly = 0) { sanitizerMock.sanitize(any()) }
        verify(exactly = 0) { headerInjectorMock.inject(any(), any(), any()) }

        // verify that workspace initialization and engine cleanup were still performed even if there were no files to process
        verify(exactly = 1) { workspaceMock.initialize() }
        verify(exactly = 1) { engineMock.removeBloat() }
    }
}
