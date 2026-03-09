package ax.stardust.plainjane

private val isWindowsOS = System.getProperty("os.name").contains("windows", ignoreCase = true)
private val forceEmojis = System.getenv("FORCE_EMOJIS") == "true"
private val useAscii = isWindowsOS && !forceEmojis

/**
 * Cross-platform console UI markers for Plain Jane.
 *
 * To maintain a "drama-free" and clean terminal experience, this object automatically
 * serves padded ASCII tags (e.g., `[run]  `) on Windows environments where legacy
 * terminals (like PowerShell or CMD) struggle with UTF-8 emoji rendering.
 * On capable systems (Mac/Linux), or when forced via the `FORCE_EMOJIS` environment
 * variable, it uses standard emojis (e.g., `🚀`).
 *
 * All ASCII fallbacks are intentionally padded to ensure perfect
 * vertical alignment in standard output.
 */
@Suppress("ktlint:standard:no-multi-spaces")
object UI {
    val ROCKET   = if (useAscii) "==>  " else "🚀"
    val CHECK    = if (useAscii) "  v  " else "✅"
    val SWEEP    = if (useAscii) "  ~  " else "🧹"
    val SOAP     = if (useAscii) "  ~  " else "🧼"
    val TRASH    = if (useAscii) "  -  " else "🗑️"
    val PACKAGE  = if (useAscii) "  +  " else "📦"
    val DOC      = if (useAscii) "  +  " else "📄"
    val MONKEY   = if (useAscii) "  >  " else "🙈"
    val STARS    = if (useAscii) "***  " else "✨"
    val BUG      = if (useAscii) "  !  " else "🐞"
    val WARNING  = if (useAscii) "  !  " else "⚠️"
    val ERROR    = if (useAscii) "  x  " else "❌"
}
