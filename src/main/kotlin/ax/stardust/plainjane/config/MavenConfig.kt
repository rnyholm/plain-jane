package ax.stardust.plainjane.config

/**
 * Encapsulates the Maven coordinates and distribution management settings.
 *
 * This configuration acts as the blueprint for generating a valid `pom.xml` and
 * is also utilized to populate the generated `README.md` with accurate dependency
 * integration snippets. It strictly requires the standard Maven trinity (Group,
 * Artifact, Version), while repository distribution settings remain optional.
 *
 * @property groupId The Maven group ID (e.g., "com.example.api"), defining the project's namespace.
 * @property artifactId The Maven artifact ID (e.g., "payment-client"). Used as the module name in both the POM and README.
 * @property artifactVersion The specific version of the generated library (e.g., "1.0.0-SNAPSHOT").
 * @property distributionId An optional repository identifier used in `<distributionManagement>`. Typically corresponds to a server ID in the user's `settings.xml`.
 * @property distributionName An optional, human-readable name for the distribution repository.
 * @property distributionUrl An optional URL pointing to the target deployment repository (e.g., Nexus or Artifactory).
 */
data class MavenConfig(
    val groupId: String,
    val artifactId: String,
    val artifactVersion: String,
    val distributionId: String?,
    val distributionName: String?,
    val distributionUrl: String?,
)
