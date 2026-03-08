package ax.stardust.plainjane.cli

import ax.stardust.plainjane.ModelOrchestrator
import ax.stardust.plainjane.TestData.ARTIFACT_ID
import ax.stardust.plainjane.TestData.DISTRIBUTION_ID
import ax.stardust.plainjane.TestData.DISTRIBUTION_URL
import ax.stardust.plainjane.TestData.PACKAGE_NAME
import ax.stardust.plainjane.TestData.PETSTORE_URL
import ax.stardust.plainjane.TestData.extractFileName
import com.github.ajalt.clikt.testing.test
import io.mockk.every
import io.mockk.mockkConstructor
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals

@Suppress("ktlint:standard:argument-list-wrapping")
class PlainJaneCommandTest {
    @BeforeEach
    fun setup() {
        mockkConstructor(ModelOrchestrator::class)
    }

    @AfterEach
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `should print help and banner on empty args`() {
        val result = PlainJaneCommand().test(emptyList())

        assertEquals(0, result.statusCode)
        assertContains(result.output, "The no-nonsense OpenAPI generator")
        assertContains(result.output, "Usage: plain-jane")
    }

    @Test
    fun `should fail if input is local file but does not exist`(
        @TempDir tempDir: File,
    ) {
        val nonExistingFile = File(tempDir, "missing-api.yaml")

        val result =
            PlainJaneCommand().test(
                listOf(
                    "-i", nonExistingFile.absolutePath,
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                ),
            )
        assertEquals(1, result.statusCode)
        assertContains(result.stderr, "File \"${nonExistingFile.absolutePath}\" does not exist")
    }

    @Test
    fun `should fail if input path is a directory instead of a file`(
        @TempDir tempDir: File,
    ) {
        val result =
            PlainJaneCommand().test(
                listOf(
                    "-i", tempDir.absolutePath, // passing a directory instead of a file
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                ),
            )
        assertEquals(1, result.statusCode)
        assertContains(result.stderr, "Path \"${tempDir.absolutePath}\" is a directory, not a file")
    }

    @Test
    fun `should fail if artifact-id is missing when generating POM via --scaffold or --with-pom`(
        @TempDir tempDir: File,
    ) {
        val expectedError = "An artifact ID (--artifact-id) is required when generating a POM"

        var result =
            PlainJaneCommand().test(
                listOf(
                    "-i", PETSTORE_URL, // using url bypasses file validation
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                    "--scaffold", // triggers POM generation but missing artifact ID (-a/--artifact-id)
                ),
            )
        assertEquals(1, result.statusCode)
        assertContains(result.stderr, expectedError)

        result =
            PlainJaneCommand().test(
                listOf(
                    "-i", PETSTORE_URL,
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                    "--with-pom", // triggers POM generation but missing artifact ID (-a/--artifact-id)
                ),
            )
        assertEquals(1, result.statusCode)
        assertContains(result.stderr, expectedError)
    }

    @Test
    fun `should fail if distribution params are used without generating POM`(
        @TempDir tempDir: File,
    ) {
        val result =
            PlainJaneCommand().test(
                listOf(
                    "-i", PETSTORE_URL,
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                    "--dist-id", DISTRIBUTION_ID,
                    "--dist-url", DISTRIBUTION_URL,
                    // note: neither --with-pom nor --scaffold is present here
                ),
            )
        assertEquals(1, result.statusCode)
        assertContains(result.stderr, "Distribution parameters require the --with-pom or --scaffold flag")
    }

    @Test
    fun `should fail on incomplete distribution configuration`(
        @TempDir tempDir: File,
    ) {
        val expectedError = "Incomplete distribution configuration"

        var result =
            PlainJaneCommand().test(
                listOf(
                    "-i", PETSTORE_URL,
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                    "-a", ARTIFACT_ID,
                    "--with-pom",
                    "--dist-id", DISTRIBUTION_ID, // missing distribution url
                ),
            )
        assertEquals(1, result.statusCode)
        assertContains(result.stderr, expectedError)

        result =
            PlainJaneCommand().test(
                listOf(
                    "-i", PETSTORE_URL,
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                    "--artifact-id", ARTIFACT_ID,
                    "--with-pom",
                    "--dist-url", DISTRIBUTION_URL, // missing distribution ID
                ),
            )
        assertEquals(1, result.statusCode)
        assertContains(result.stderr, expectedError)
    }

    @Test
    fun `should successfully parse args and delegate to orchestrator on valid input`(
        @TempDir tempDir: File,
    ) {
        // validation of input args will pass in this test, so we need to
        // mock the actual orchestration to avoid running the full pipeline
        every { anyConstructed<ModelOrchestrator>().orchestrate() } returns
            ModelOrchestrator.Result(totalCount = 1, sanitizedCount = 1, failedCount = 0)

        val result =
            PlainJaneCommand().test(
                listOf(
                    "-i", PETSTORE_URL,
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                    "-a", ARTIFACT_ID,
                    "--with-pom",
                ),
            )

        assertEquals(0, result.statusCode, "Unexpected non-zero exit code for valid input: ${result.stderr}")

        // verify that orchestrate was called
        verify(exactly = 1) { anyConstructed<ModelOrchestrator>().orchestrate() }

        // verify output to cli
        assertContains(result.output, "Input:   ${extractFileName(PETSTORE_URL)}")
        assertContains(result.output, "Package: $PACKAGE_NAME")
        assertContains(result.output, "Extras:  pom.xml")
    }

    @Test
    fun `should catch exceptions from orchestrator, print debug info and exit with status 1`(
        @TempDir tempDir: File,
    ) {
        val expectedError = "The OpenAPI specification is severely malformed"
        every { anyConstructed<ModelOrchestrator>().orchestrate() } throws RuntimeException(expectedError)

        // Act
        val result =
            PlainJaneCommand().test(
                listOf(
                    "-i", PETSTORE_URL,
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                    "--debug", // enable debug to test stacktrace printing
                ),
            )

        assertEquals(1, result.statusCode) // clikt should throw ProgramResult(1) in this test
        verify(exactly = 1) { anyConstructed<ModelOrchestrator>().orchestrate() }

        // verify that the error message is printed in stderr along with the stack trace
        assertContains(result.stderr, "❌ Fatal Error: $expectedError")
        assertContains(result.stderr, "java.lang.RuntimeException: $expectedError")
    }

    @Test
    fun `should successfully generate purely models with no extras`(
        @TempDir tempDir: File,
    ) {
        every { anyConstructed<ModelOrchestrator>().orchestrate() } returns
            ModelOrchestrator.Result(totalCount = 5, sanitizedCount = 0, failedCount = 0)

        val result =
            PlainJaneCommand().test(
                listOf(
                    "-i", PETSTORE_URL,
                    "-o", tempDir.absolutePath,
                    "-p", PACKAGE_NAME,
                    // no -artifact-id, no --scaffold, no extras whatsoever, just pure model generation
                ),
            )

        assertEquals(0, result.statusCode)
        verify(exactly = 1) { anyConstructed<ModelOrchestrator>().orchestrate() }

        // verify that the output is missing extras section when no extras are generated
        val outputMissingExtras = !result.output.contains("Extras:")
        assert(outputMissingExtras) { "Output should not contain 'extras' when none are generated" }
    }
}
