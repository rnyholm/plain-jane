package ax.stardust.plainjane.engine

import ax.stardust.plainjane.UI
import ax.stardust.plainjane.config.ExtrasConfig
import ax.stardust.plainjane.config.IOConfig
import ax.stardust.plainjane.config.MavenConfig
import ax.stardust.plainjane.config.ModelConfig
import ax.stardust.plainjane.config.ToolVersions
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Responsible for generating supplementary project files that accompany the sanitized models.
 *
 * While the [OpenApiEngine] and [JavaSanitizer] handle the actual source code, this class
 * elevates the output directory from a simple folder of Java files into a fully integrated,
 * ready-to-publish project. It conditionally generates build scripts, documentation, and
 * version control ignores based on the user's [ExtrasConfig].
 *
 * @property mavenConfig Configuration for Maven artifact details (groupId, artifactId, etc.) and distribution management.
 * @property modelConfig Configuration detailing the domain models (e.g., the target package name).
 * @property extrasConfig Flags determining which supplementary files should be generated.
 * @property ioConfig Configuration for file system paths, specifically the target output directory.
 * @property toolVersions Contains version metadata for Plain Jane and the underlying generation engine.
 * @property log A logging function to report generation progress back to the user.
 */
class ExtrasGenerator(
    private val mavenConfig: MavenConfig,
    private val modelConfig: ModelConfig,
    private val extrasConfig: ExtrasConfig,
    private val ioConfig: IOConfig,
    private val toolVersions: ToolVersions,
    private val log: (String) -> Unit,
) {
    /**
     * Conditionally triggers the generation of all requested supplementary files.
     *
     * @param apiTitle The title of the API as parsed from the OpenAPI specification (used in documentation).
     * @param apiVersion The version of the API as parsed from the OpenAPI specification (used in documentation).
     */
    fun generate(
        apiTitle: String,
        apiVersion: String,
    ) {
        if (extrasConfig.generatePom) {
            generatePomXml()
        }
        if (extrasConfig.generateReadme) {
            generateReadme(apiTitle = apiTitle, apiVersion = apiVersion)
        }
        if (extrasConfig.generateGitignore) {
            generateGitignore()
        }
    }

    private fun generatePomXml() {
        // using buildString to conditionally build up the content, less efficient than template but the code is more
        // readable and maintainable than a big template with multiple ifs inside
        val content =
            buildString {
                appendLine("""<?xml version="1.0" encoding="UTF-8"?>""")
                appendLine("""<project xmlns="http://maven.apache.org/POM/4.0.0"""")
                appendLine("""         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"""")
                appendLine(
                    """         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">""",
                )
                appendLine("""    <modelVersion>4.0.0</modelVersion>""")
                appendLine()
                appendLine("""    <properties>""")
                appendLine("""        <maven.compiler.source>8</maven.compiler.source>""")
                appendLine("""        <maven.compiler.target>8</maven.compiler.target>""")
                appendLine("""        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>""")
                appendLine("""    </properties>""")
                appendLine()
                appendLine("""    <groupId>${mavenConfig.groupId}</groupId>""")
                appendLine("""    <artifactId>${mavenConfig.artifactId}</artifactId>""")
                appendLine("""    <version>${mavenConfig.artifactVersion}</version>""")
                appendLine()
                appendLine("""    <build>""")
                appendLine("""        <sourceDirectory>.</sourceDirectory>""")
                appendLine("""    </build>""")

                if (mavenConfig.distributionId != null && mavenConfig.distributionUrl != null) {
                    appendLine()
                    appendLine("    <distributionManagement>")
                    appendLine("        <repository>")
                    appendLine("            <id>${mavenConfig.distributionId}</id>")
                    if (mavenConfig.distributionName != null) {
                        appendLine("            <name>${mavenConfig.distributionName}</name>")
                    }
                    appendLine("            <url>${mavenConfig.distributionUrl}</url>")
                    appendLine("        </repository>")
                    appendLine("    </distributionManagement>")
                }

                appendLine("""</project>""")
            }

        File(ioConfig.output, "pom.xml").writeText(content)
        log("${UI.PACKAGE} Generated pom.xml")
    }

    private fun generateReadme(
        apiTitle: String,
        apiVersion: String,
    ) {
        val generatedDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))

        val content =
            """
            # ${mavenConfig.artifactId}
            
            This library contains dependency-free Java models generated from an OpenAPI specification.
            
            The code has been orchestrated by **Plain Jane**, ensuring that all models are "sanitized" from heavy framework 
            annotations and external library dependencies. This makes the library ideal for projects that require a clean 
            domain model or wish to avoid transitive dependency bloat.
            
            ## 🛠️ Technical    
            - **Generated by:** Plain Jane v${toolVersions.plainJaneVersion}
            - **Engine:** OpenAPI Generator v${toolVersions.engineVersion}
            - **Target Platform:** Java 8+
            - **API Title:** $apiTitle
            - **API Version:** $apiVersion
            - **Generated on:** $generatedDate

            ## 🚀 Building & Publishing
            If this project was generated with a `pom.xml` (`--scaffold` or `--with-pom`), you can easily build and publish the artifact using Maven.

            To compile the models and install them into your local Maven repository (for use in other local projects):
            ```bash
            mvn clean install
            ```

            To package and deploy the artifacts to your remote registry (ensure `distributionManagement` is configured):
            ```bash
            mvn clean deploy
            ```

            ## 🔗 Integration
            This project uses a flat source structure where the package hierarchy starts directly at the root. 
            If the code was generated with pom the included `pom.xml` is pre-configured with `<sourceDirectory>.</sourceDirectory>`.
            
            ### 📦 Maven
            If you publish this artifact to a repository, include it in your project as follows:
            
            ```xml
            <dependency>
                <groupId>${mavenConfig.groupId}</groupId>
                <artifactId>${mavenConfig.artifactId}</artifactId>
                <version>${mavenConfig.artifactVersion}</version>
            </dependency>
            ```
            
            ## 🖐️ Manual Usage
            You can also copy the `${modelConfig.pkg.replace('.', '/')}` directory directly into your 
            project's source tree if you prefer to manage the models as part of your internal codebase.
            
            ## 💡 Key Features
            - **Zero Annotations:** No `@JsonProperty`, `@Schema`, or other framework-specific metadata.
            - **Zero Dependencies:** Does not require Jackson, Gson, Jakarta EE, or any other library to compile, JRE is enough.
            - **Pure Java Types:** Uses only standard Java types (e.g., `java.util.List`, `java.time.OffsetDateTime`).
            
            ## 📝 Notes
            ### Polymorphism & JSON Serialization
            Because Plain Jane generates pure Java, she does not dictate how you serialize or deserialize your data. 

            If your OpenAPI specification uses polymorphism (e.g., `oneOf`, `anyOf`, or `discriminator`), Plain Jane will 
            generate the correct class hierarchy (inheritance). However, **you must configure your JSON mapper** 
            (such as Jackson Subtypes/Mixins or Gson runtime adapters) to handle the subtype mapping manually. 
            Plain Jane provides the clean POJOs, but you own the parsing logic!
            
            ---
            *Generated with ❤️ by Plain Jane*
            """.trimIndent()

        File(ioConfig.output, "README.md").writeText(content)
        log("${UI.DOC} Generated README.md")
    }

    private fun generateGitignore() {
        File(ioConfig.output, ".gitignore").writeText(
            """
            /target
            /.idea
            *.iml
            .DS_Store
            """.trimIndent(),
        )
        log("${UI.MONKEY} Generated .gitignore")
    }
}
