package ax.stardust.plainjane.config

/**
 * Configuration settings specific to the generated domain models.
 *
 * Currently, this configuration solely determines the target package namespace.
 * However, it serves as an extensible container, perfectly positioned to hold any
 * future model-specific generation flags (such as toggling Java Records, Builder
 * patterns, or custom validation annotations) without breaking the existing API.
 *
 * @property pkg The fully qualified Java package name where the sanitized models will be placed (e.g., "com.example.api.models").
 */
data class ModelConfig(
    val pkg: String,
)
