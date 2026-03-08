package ax.stardust.plainjane

object TestData {
    // core parameters
    const val PACKAGE_NAME = "ax.stardust.plainjane.test"
    const val PETSTORE_URL = "https://petstore.swagger.io/v2/swagger.json"

    // maven configuration
    const val ARTIFACT_ID = "petstore"
    const val ARTIFACT_VERSION = "1.0.1"
    const val DISTRIBUTION_ID = "petstore"
    const val DISTRIBUTION_NAME = "petstore"
    const val DISTRIBUTION_URL = "https://nexus.stardust.ax/repository/maven-releases/"

    // versions
    const val PLAIN_JANE_VERSION = "2.1.3"
    const val OPENAPI_GENERATOR_VERSION = "7.4.0"
    const val FALLBACK_PLAIN_JANE_VERSION = "0.0.0-DEV"
    const val FALLBACK_OPEN_API_GENERATOR_VERSION = "unknown"

    // api
    const val API_TITLE = "Swagger Petstore"
    const val API_VERSION = "1.0.7"

    fun extractFileName(url: String): String = url.substringAfterLast("/").substringAfterLast("\\")
}
