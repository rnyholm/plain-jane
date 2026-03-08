package ax.stardust.plainjane.cli

import ax.stardust.plainjane.ModelOrchestrator
import ax.stardust.plainjane.config.ExtrasConfig
import ax.stardust.plainjane.config.IOConfig
import ax.stardust.plainjane.config.Input
import ax.stardust.plainjane.config.MavenConfig
import ax.stardust.plainjane.config.ModelConfig
import ax.stardust.plainjane.config.RuntimeOptions
import ax.stardust.plainjane.engine.ExtrasGenerator
import ax.stardust.plainjane.engine.HeaderInjector
import ax.stardust.plainjane.engine.JavaSanitizer
import ax.stardust.plainjane.engine.OpenApiEngine
import ax.stardust.plainjane.engine.WorkspaceInitializer
import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.output.MordantHelpFormatter
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.options.validate
import com.github.ajalt.clikt.parameters.options.versionOption
import com.github.ajalt.clikt.parameters.types.file
import java.io.File

/**
 * The primary entry point and CLI definition for the Plain Jane generator.
 *
 * Built on top of the Clikt framework, this class defines the user-facing command-line
 * interface, handles argument parsing, and enforces "fail-fast" validation rules before
 * any heavy lifting begins.
 *
 * Once the user input is validated and the configuration objects ([IOConfig], [MavenConfig],
 * [ExtrasConfig], [ModelConfig], [RuntimeOptions]) are populated, this class acts as the
 * Composition Root. It instantiates the underlying engines and tools, wires them together
 * via dependency injection, and delegates the execution to the [ModelOrchestrator].
 */
fun main(args: Array<String>) = PlainJaneCommand().main(args)

class PlainJaneCommand : CliktCommand(name = "plain-jane") {
    override val printHelpOnEmptyArgs = true

    private val toolVersions = VersionLoader.load()

    init {
        versionOption(
            toolVersions.plainJaneVersion,
            names = setOf("--version"),
            message = { "Plain Jane version: $it" },
        )
        context {
            helpFormatter = { ctx ->
                echo(BANNER)
                MordantHelpFormatter(ctx, showDefaultValues = true)
            }
        }
    }

    // --- I/O options ---
    private val uri by option(
        "-i",
        "--input",
        help = "Path to local file or URL to the OpenAPI specification",
    ).required().validate { value ->
        val isUrl = value.startsWith("http://") || value.startsWith("https://")
        if (!isUrl) {
            val file = File(value)
            if (!file.exists()) fail("File \"$value\" does not exist.")
            if (!file.isFile) fail("Path \"$value\" is a directory, not a file.")
        }
    }

    private val out by option(
        "-o",
        "--output",
        help = "Path to the output directory where generated code is saved",
    ).file(canBeFile = false).required()

    private val clean by option(
        "-c",
        "--clean",
        help = "Wipe the output directory completely before generating new files",
    ).flag(default = false)

    // --- Model configuration options ---
    private val packageName by option("-p", "--package", help = "The Java package name (e.g. com.example.models)")
        .required()

    // --- Maven configuration options ---
    private val groupId by option(
        "-g",
        "--group-id",
        help = "Maven group ID (defaults to the package name)",
    )

    private val artifactId by option(
        "-a",
        "--artifact-id",
        help = "Maven artifact ID (required if generating a POM)",
    )

    private val artifactVersion by option(
        "-av",
        "--artifact-version",
        help = "Version of the generated Maven artifact (e.g. 1.0.0)",
    ).default("1.0.0-SNAPSHOT")

    private val distributionId by option("--dist-id", help = "Distribution repository ID")
    private val distributionName by option("--dist-name", help = "Distribution repository name")
    private val distributionUrl by option("--dist-url", help = "Distribution repository URL")

    // --- Extras generation ---
    private val scaffold by option(
        "-s",
        "--scaffold",
        help = "Generate Maven project structure (pom.xml, README.md & .gitignore)",
    ).flag(default = false)

    private val withPom by option("--with-pom", help = "Generate pom.xml")
        .flag(default = false)

    private val withReadme by option("--with-readme", help = "Generate README.md")
        .flag(default = false)

    private val withGitignore by option("--with-gitignore", help = "Generate .gitignore")
        .flag(default = false)

    // --- Runtime configuration ---
    private val debug by option("-d", "--debug", help = "Enable debug logging to see underlying engine output")
        .flag(default = false)

    override fun run() {
        val generatePom = withPom || scaffold
        val generateReadme = withReadme || scaffold
        val generateGitignore = withGitignore || scaffold

        // validating distribution configuration early to fail fast
        validateMavenConfiguration(shouldGeneratePom = generatePom)

        // bootstrapping
        val ioConfig =
            IOConfig(
                input = Input(uri = uri),
                output = out,
                clean = clean,
            )

        val mavenConfig =
            MavenConfig(
                groupId = groupId ?: packageName,
                // we know that we either have artifact-id (pom is to be generated) or we don't need it,
                // so it's safe to default to package name
                artifactId = artifactId ?: packageName.substringAfterLast('.'),
                artifactVersion = artifactVersion,
                distributionId = distributionId,
                distributionName = distributionName,
                distributionUrl = distributionUrl,
            )

        val extrasConfig =
            ExtrasConfig(
                generatePom = generatePom,
                generateReadme = generateReadme,
                generateGitignore = generateGitignore,
            )

        val modelConfig = ModelConfig(pkg = packageName)
        val runtimeOptions = RuntimeOptions(debug = debug)

        // composition root - wiring all the components together and passing the configuration down to them
        val logger: (String) -> Unit = { message -> echo(message) }
        val sanitizer = JavaSanitizer(runtimeOptions = runtimeOptions, log = logger)
        val headerInjector = HeaderInjector(toolVersions = toolVersions)

        val workspace =
            WorkspaceInitializer(
                ioConfig = ioConfig,
                pkg = modelConfig.pkg,
                log = logger,
            )

        val engine =
            OpenApiEngine(
                ioConfig = ioConfig,
                modelConfig = modelConfig,
                runtimeOptions = runtimeOptions,
                toolVersions = toolVersions,
                artifactId = mavenConfig.artifactId,
                log = logger,
            )

        val extrasGenerator =
            ExtrasGenerator(
                mavenConfig = mavenConfig,
                modelConfig = modelConfig,
                extrasConfig = extrasConfig,
                ioConfig = ioConfig,
                toolVersions = toolVersions,
                log = logger,
            )

        val orchestrator =
            ModelOrchestrator(
                workspace = workspace,
                engine = engine,
                sanitizer = sanitizer,
                headerInjector = headerInjector,
                extrasGenerator = extrasGenerator,
                log = logger,
            )

        try {
            printWelcomeBanner(
                inputName = ioConfig.input.name,
                pkg = modelConfig.pkg,
                extrasConfig = extrasConfig,
            )

            val orchestrationResult = orchestrator.orchestrate() // do the job!
            val message =
                if (orchestrationResult.failedCount > 0) {
                    "\n⚠️ Done! Generated ${orchestrationResult.sanitizedCount} clean models and " +
                        "${orchestrationResult.failedCount} raw models (due to parsing errors) " +
                        "in ${ioConfig.output.absolutePath}"
                } else {
                    "\n✨ Done! Generated ${orchestrationResult.sanitizedCount} clean models in ${ioConfig.output.absolutePath}"
                }
            echo(message)
        } catch (e: Exception) {
            echo("\n❌ Fatal Error: ${e.message}", err = true)
            if (runtimeOptions.debug) {
                echo(e.stackTraceToString(), err = true)
            }
            throw ProgramResult(1)
        }
    }

    private fun validateMavenConfiguration(shouldGeneratePom: Boolean) {
        // artifact id is required if pom should be generated
        if (shouldGeneratePom && artifactId.isNullOrBlank()) {
            throw UsageError("An artifact ID (--artifact-id) is required when generating a POM (--with-pom or --scaffold).")
        }

        // validate distribution parameters
        val hasDistId = distributionId != null
        val hasDistUrl = distributionUrl != null
        val hasDistName = distributionName != null

        if (hasDistId || hasDistUrl || hasDistName) {
            if (!shouldGeneratePom) {
                throw UsageError("Distribution parameters require the --with-pom or --scaffold flag to be set.")
            }
            if (!(hasDistId && hasDistUrl)) {
                throw UsageError("Incomplete distribution configuration. You must provide at least --dist-id and --dist-url.")
            }
        }
    }

    private fun printWelcomeBanner(
        inputName: String,
        pkg: String,
        extrasConfig: ExtrasConfig,
    ) {
        echo(BANNER)
        echo("Input:   $inputName")
        echo("Package: $pkg")

        val desiredFiles =
            listOfNotNull(
                "pom.xml".takeIf { extrasConfig.generatePom },
                "README.md".takeIf { extrasConfig.generateReadme },
                ".gitignore".takeIf { extrasConfig.generateGitignore },
            )

        if (desiredFiles.isNotEmpty()) {
            val result =
                when {
                    desiredFiles.size <= 2 -> desiredFiles.joinToString(" and ")
                    else -> desiredFiles.dropLast(1).joinToString(", ") + " and " + desiredFiles.last()
                }
            echo("Extras:  $result")
        }
        echo()
    }

    companion object {
        private val BANNER =
            """
             ____  _       _             _                  
            |  _ \| | __ _(_)_ __       | | __ _ _ __   ___ 
            | |_) | |/ _` | | '_ \   _  | |/ _` | '_ \ / _ \
            |  __/| | (_| | | | | | | |_| | (_| | | | |  __/
            |_|   |_|\__,_|_|_| |_|  \___/ \__,_|_| |_|\___|
            
            The no-nonsense OpenAPI generator.

            """.trimIndent()
    }
}
