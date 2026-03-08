package ax.stardust.plainjane.config

/**
 * Encapsulates execution-time behavioral flags for the Plain Jane CLI.
 *
 * This configuration object serves as a dedicated container for global runtime settings.
 * By bundling these options together, it prevents the CLI command from passing multiple
 * disconnected boolean flags down the execution chain, ensuring a clean and extensible API
 * for the underlying orchestration engines.
 *
 * @property debug If true, enables verbose console output. This includes revealing the underlying
 * OpenAPI Generator engine logs, showing AST manipulation warnings, and printing full stack
 * traces upon fatal errors.
 */
data class RuntimeOptions(
    val debug: Boolean,
)
