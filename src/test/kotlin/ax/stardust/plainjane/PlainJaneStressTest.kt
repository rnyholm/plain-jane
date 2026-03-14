package ax.stardust.plainjane

import ax.stardust.plainjane.cli.PlainJaneCommand
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The ultimate endurance and stability test for the Plain Jane generation pipeline.
 *
 * This test utilizes the massive and highly complex Stripe OpenAPI specification to ensure
 * that the underlying generation engine and the [ax.stardust.plainjane.engine.JavaSanitizer]
 * can handle extreme edge cases. It verifies that Plain Jane can ingest a ~5MB specification,
 * generate thousands of classes, strip away all framework bloat, and output 100% syntactically
 * valid, dependency-free Java code.
 */
class PlainJaneStressTest {
    /**
     * Executes the full CLI pipeline against the Stripe API specification and programmatically
     * compiles the entire resulting codebase.
     *
     * The test runs in three phases:
     * 1. **Generation:** Invokes the CLI to generate models from `stripe-openapi.json`.
     * 2. **Verification:** Asserts that a massive amount of files (>500) were successfully created.
     * 3. **The Ultimate Test (Compilation):** Feeds every single generated `.java` file into
     * Java's native system compiler. If the AST-sanitization missed a bug, left an orphaned
     * import, or broke the syntax, the compilation will fail and print the exact offending line.
     *
     * @param tempDir A temporary directory provided by JUnit to house the generated thousands of files.
     */
    @Test
    fun `should successfully generate and compile the massive Stripe API spec`(
        @TempDir tempDir: File,
    ) {
        val specFileName = "stripe-openapi.json"
        val resourceUrl =
            this::class.java.classLoader.getResource(specFileName)
                ?: throw IllegalStateException("Could not find $specFileName in test resources. Have you downloaded it?")
        val specFile = File(resourceUrl.toURI())

        val outputDir = File(tempDir, "stripe-output")
        val packageName = "com.stripe.api.models"

        println("🚀 Starting stress test with Stripe API (this may take a few seconds)...")
        val startTime = System.currentTimeMillis()

        // run CLI with the Stripe specification
        val result =
            PlainJaneCommand().test(
                listOf(
                    "-i",
                    specFile.absolutePath,
                    "-o",
                    outputDir.absolutePath,
                    "-p",
                    packageName,
                ),
            )

        assertEquals(0, result.statusCode, "CLI crashed during generation. Error: ${result.stderr}")

        // verify that we got a huge amount of files
        val expectedModelDir = File(outputDir, packageName.replace('.', File.separatorChar))
        val allJavaFiles = expectedModelDir.listFiles { _, name -> name.endsWith(".java") }?.toList() ?: emptyList()

        println(
            "✅ Generation & Scrubbing complete! Found ${allJavaFiles.size} clean Java files in ${System.currentTimeMillis() - startTime} ms.",
        )
        assertTrue(allJavaFiles.size > 500, "Expected hundreds of files from Stripe, but only got ${allJavaFiles.size}")

        // the ultimate test: compile the stripe library programmatically
        println("🔨 Compiling ${allJavaFiles.size} files with Javac...")
        val compileStartTime = System.currentTimeMillis()

        val compiler = ToolProvider.getSystemJavaCompiler()
        val diagnostics = DiagnosticCollector<JavaFileObject>()
        val fileManager = compiler.getStandardFileManager(diagnostics, null, null)

        val compilationUnits = fileManager.getJavaFileObjectsFromFiles(allJavaFiles)
        val task = compiler.getTask(null, fileManager, diagnostics, null, null, compilationUnits)

        val success = task.call()

        if (!success) {
            val errors =
                diagnostics.diagnostics
                    .take(20)
                    .map { diagnostic ->
                        val lineNumber = diagnostic.lineNumber.toInt()
                        val file = File(diagnostic.source?.toUri() ?: return@map "Unknown error")
                        val offendingLine =
                            if (file.exists() && lineNumber > 0) {
                                file.readLines().getOrNull(lineNumber - 1)?.trim() ?: "<Couldn't read line>"
                            } else {
                                "<File missing>"
                            }

                        "File: ${file.name} (Line $lineNumber)\nError: ${diagnostic.getMessage(null)}\nCode: $offendingLine\n"
                    }.joinToString("\n")

            throw AssertionError("The Stripe code could not be compiled!\nShowing the first 20 errors:\n\n$errors")
        }

        fileManager.close()
        println("🎉 Compilation of Stripe API succeeded without any errors in ${System.currentTimeMillis() - compileStartTime} ms!")
    }
}
