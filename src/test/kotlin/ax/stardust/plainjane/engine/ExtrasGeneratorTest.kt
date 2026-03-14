package ax.stardust.plainjane.engine

import ax.stardust.plainjane.TestData.API_TITLE
import ax.stardust.plainjane.TestData.API_VERSION
import ax.stardust.plainjane.TestData.ARTIFACT_ID
import ax.stardust.plainjane.TestData.ARTIFACT_VERSION
import ax.stardust.plainjane.TestData.DISTRIBUTION_ID
import ax.stardust.plainjane.TestData.DISTRIBUTION_NAME
import ax.stardust.plainjane.TestData.DISTRIBUTION_URL
import ax.stardust.plainjane.TestData.OPENAPI_GENERATOR_VERSION
import ax.stardust.plainjane.TestData.PACKAGE_NAME
import ax.stardust.plainjane.TestData.PETSTORE_URL
import ax.stardust.plainjane.TestData.PLAIN_JANE_VERSION
import ax.stardust.plainjane.UI
import ax.stardust.plainjane.config.ExtrasConfig
import ax.stardust.plainjane.config.IOConfig
import ax.stardust.plainjane.config.Input
import ax.stardust.plainjane.config.MavenConfig
import ax.stardust.plainjane.config.ModelConfig
import ax.stardust.plainjane.config.ToolVersions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExtrasGeneratorTest {
    private lateinit var capturedLogs: MutableList<String>
    private val logger: (String) -> Unit = { capturedLogs.add(it) }

    private lateinit var baseMavenConfig: MavenConfig
    private lateinit var baseModelConfig: ModelConfig
    private lateinit var baseToolVersions: ToolVersions
    private lateinit var baseIOConfig: IOConfig

    @BeforeEach
    fun setup(
        @TempDir tempDir: File,
    ) {
        capturedLogs = mutableListOf()

        baseMavenConfig =
            MavenConfig(
                groupId = PACKAGE_NAME,
                artifactId = ARTIFACT_ID,
                artifactVersion = ARTIFACT_VERSION,
                // setting distribution to null by default to test the base POM generation without distribution block
                distributionId = null,
                distributionName = null,
                distributionUrl = null,
            )
        baseModelConfig = ModelConfig(pkg = PACKAGE_NAME)
        baseToolVersions = ToolVersions(plainJaneVersion = PLAIN_JANE_VERSION, engineVersion = OPENAPI_GENERATOR_VERSION)
        baseIOConfig = IOConfig(input = Input(uri = PETSTORE_URL), output = tempDir, clean = false)
    }

    @Test
    fun `should not generate any files when all extras config flags are false`() {
        val config = ExtrasConfig(generatePom = false, generateReadme = false, generateGitignore = false)
        val generator =
            ExtrasGenerator(
                mavenConfig = baseMavenConfig,
                modelConfig = baseModelConfig,
                extrasConfig = config,
                ioConfig = baseIOConfig,
                toolVersions = baseToolVersions,
                log = logger,
            )

        generator.generate(apiTitle = API_TITLE, apiVersion = API_VERSION)

        // no files should have been created in the output directory
        val files = baseIOConfig.output.listFiles() ?: emptyArray()
        assertEquals(0, files.size, "Output directory should be empty")
        assertTrue(capturedLogs.isEmpty(), "Should not log anything")
    }

    @Test
    fun `should generate valid pom xml without distribution block`() {
        val config = ExtrasConfig(generatePom = true, generateReadme = false, generateGitignore = false)
        val generator =
            ExtrasGenerator(
                mavenConfig = baseMavenConfig,
                modelConfig = baseModelConfig,
                extrasConfig = config,
                ioConfig = baseIOConfig,
                toolVersions = baseToolVersions,
                log = logger,
            )

        generator.generate(apiTitle = API_TITLE, apiVersion = API_VERSION)

        // pom without distribution block should have been generated with correct metadata
        val pomFile = File(baseIOConfig.output, "pom.xml")
        assertTrue(pomFile.exists())

        val content = pomFile.readText()
        assertContains(content, "<groupId>$PACKAGE_NAME</groupId>")
        assertContains(content, "<artifactId>$ARTIFACT_ID</artifactId>")
        // note: this is the artifact version from the config, not the API version
        assertContains(content, "<version>$ARTIFACT_VERSION</version>")
        assertFalse(content.contains("<distributionManagement>"), "Should not contain distribution block if omitted")

        assertContains(capturedLogs, "${UI.PACKAGE} Generated pom.xml")
    }

    @Test
    fun `should generate pom xml with complete distribution block`() {
        val mavenConfigWithDist =
            baseMavenConfig.copy(
                distributionId = DISTRIBUTION_ID,
                distributionName = DISTRIBUTION_NAME,
                distributionUrl = DISTRIBUTION_URL,
            )
        val config = ExtrasConfig(generatePom = true, generateReadme = false, generateGitignore = false)
        val generator =
            ExtrasGenerator(
                mavenConfig = mavenConfigWithDist,
                modelConfig = baseModelConfig,
                extrasConfig = config,
                ioConfig = baseIOConfig,
                toolVersions = baseToolVersions,
                log = logger,
            )

        generator.generate(apiTitle = API_TITLE, apiVersion = API_VERSION)

        // pom with distribution block should have been generated with correct metadata
        val pomFile = File(baseIOConfig.output, "pom.xml")
        assertTrue(pomFile.exists())

        val content = pomFile.readText()
        assertContains(content, "<distributionManagement>")
        assertContains(content, "<id>$DISTRIBUTION_ID</id>")
        assertContains(content, "<name>$DISTRIBUTION_NAME</name>")
        assertContains(content, "<url>$DISTRIBUTION_URL</url>")

        assertContains(capturedLogs, "${UI.PACKAGE} Generated pom.xml")
    }

    @Test
    fun `should generate pom xml with distribution block omitting name when not provided`() {
        val mavenConfigWithDist =
            baseMavenConfig.copy(
                distributionId = DISTRIBUTION_ID,
                distributionName = null, // no distribution name provided, should be omitted in the generated pom
                distributionUrl = DISTRIBUTION_URL,
            )
        val config = ExtrasConfig(generatePom = true, generateReadme = false, generateGitignore = false)
        val generator =
            ExtrasGenerator(
                mavenConfig = mavenConfigWithDist,
                modelConfig = baseModelConfig,
                extrasConfig = config,
                ioConfig = baseIOConfig,
                toolVersions = baseToolVersions,
                log = logger,
            )

        generator.generate(apiTitle = API_TITLE, apiVersion = API_VERSION)

        // pom with distribution block should have been generated with correct metadata
        val pomFile = File(baseIOConfig.output, "pom.xml")
        assertTrue(pomFile.exists())

        val content = File(baseIOConfig.output, "pom.xml").readText()
        assertContains(content, "<distributionManagement>")
        assertContains(content, "<id>$DISTRIBUTION_ID</id>")
        assertContains(content, "<url>$DISTRIBUTION_URL</url>")
        assertFalse(content.contains("<name>"), "Should not contain name tag when distributionName is null")

        assertContains(capturedLogs, "${UI.PACKAGE} Generated pom.xml")
    }

    @Test
    fun `should generate comprehensive README`() {
        val config = ExtrasConfig(generatePom = false, generateReadme = true, generateGitignore = false)
        val generator =
            ExtrasGenerator(
                mavenConfig = baseMavenConfig,
                modelConfig = baseModelConfig,
                extrasConfig = config,
                ioConfig = baseIOConfig,
                toolVersions = baseToolVersions,
                log = logger,
            )

        generator.generate(apiTitle = API_TITLE, apiVersion = API_VERSION)

        // README should have been generated with correct metadata and instructions
        val readmeFile = File(baseIOConfig.output, "README.md")
        assertTrue(readmeFile.exists())

        // verify that the README contains the correct data
        val content = readmeFile.readText()
        assertContains(content, "# $ARTIFACT_ID")
        assertContains(content, "Plain Jane v$PLAIN_JANE_VERSION")
        assertContains(content, "OpenAPI Generator v$OPENAPI_GENERATOR_VERSION")
        assertContains(content, "API Title:** $API_TITLE")
        assertContains(content, "API Version:** $API_VERSION")

        // verify that the date is correct (at least that today's date is there)
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        assertContains(content, "Generated on:** $today")

        // very that the dependency block contains the correct groupId
        assertContains(content, "<groupId>$PACKAGE_NAME</groupId>")

        // verify that the package path is correctly converted for manual usage
        val expectedPackagePath = PACKAGE_NAME.replace('.', '/')
        assertContains(content, "copy the `$expectedPackagePath` directory directly")

        assertContains(capturedLogs, "${UI.DOC} Generated README.md")
    }

    @Test
    fun `should generate gitignore`() {
        val config = ExtrasConfig(generatePom = false, generateReadme = false, generateGitignore = true)
        val generator =
            ExtrasGenerator(
                mavenConfig = baseMavenConfig,
                modelConfig = baseModelConfig,
                extrasConfig = config,
                ioConfig = baseIOConfig,
                toolVersions = baseToolVersions,
                log = logger,
            )

        generator.generate(apiTitle = API_TITLE, apiVersion = API_VERSION)

        // .gitignore should have been generated with basic ignores
        val gitignoreFile = File(baseIOConfig.output, ".gitignore")
        assertTrue(gitignoreFile.exists())

        val content = gitignoreFile.readText()
        assertContains(content, "/target")
        assertContains(content, "/.idea")
        assertContains(content, "*.iml")

        assertContains(capturedLogs, "${UI.MONKEY} Generated .gitignore")
    }

    @Test
    fun `should generate all files when scaffold config is fully enabled`() {
        val config = ExtrasConfig(generatePom = true, generateReadme = true, generateGitignore = true)
        val generator =
            ExtrasGenerator(
                mavenConfig = baseMavenConfig,
                modelConfig = baseModelConfig,
                extrasConfig = config,
                ioConfig = baseIOConfig,
                toolVersions = baseToolVersions,
                log = logger,
            )

        generator.generate(apiTitle = API_TITLE, apiVersion = API_VERSION)

        // verify that all files have been generated
        assertTrue(File(baseIOConfig.output, "pom.xml").exists())
        assertTrue(File(baseIOConfig.output, "README.md").exists())
        assertTrue(File(baseIOConfig.output, ".gitignore").exists())

        // verify logging for all generated files
        assertEquals(3, capturedLogs.size)
    }
}
