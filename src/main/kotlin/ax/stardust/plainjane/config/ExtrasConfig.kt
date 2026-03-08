package ax.stardust.plainjane.config

/**
 * Encapsulates the configuration flags for generating supplementary project files.
 *
 * This data class acts as a consolidated configuration object, determining which
 * extra files (like build scripts or documentation) should be scaffolded alongside
 * the generated Java models. These flags are typically derived directly from the
 * user's CLI input (e.g., `--with-pom`, `--scaffold`).
 *
 * @property generatePom If true, a `pom.xml` will be generated for Maven integration.
 * @property generateReadme If true, a `README.md` containing integration instructions will be created.
 * @property generateGitignore If true, a standard `.gitignore` file will be added to the output directory.
 */
data class ExtrasConfig(
    val generatePom: Boolean,
    val generateReadme: Boolean,
    val generateGitignore: Boolean,
)
