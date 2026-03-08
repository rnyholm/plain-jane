package ax.stardust.plainjane.engine

import ax.stardust.plainjane.config.RuntimeOptions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JavaSanitizerTest {
    private lateinit var capturedLogs: MutableList<String>
    private val logger: (String) -> Unit = { capturedLogs.add(it) }

    @BeforeEach
    fun setup() {
        capturedLogs = mutableListOf()
    }

    @Test
    fun `should remove unwanted annotations, fields, and imports`(
        @TempDir tempDir: File,
    ) {
        val dirtyCode =
            """
            package com.example.models;
            
            import java.util.Objects;
            import java.util.List;
            import com.fasterxml.jackson.annotation.JsonProperty;
            import com.fasterxml.jackson.annotation.JsonInclude;
            import io.swagger.v3.oas.annotations.media.Schema;
            import jakarta.annotation.Generated;
            
            @Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen")
            @JsonInclude(JsonInclude.Include.NON_NULL)
            public class User {
                public static final String JSON_PROPERTY_ID = "id";
                public static final String JSON_PROPERTY_NAME = "name";
                
                @Schema(description = "The user ID")
                @JsonProperty(JSON_PROPERTY_ID)
                private String id;
                
                @JsonProperty(value = JSON_PROPERTY_NAME)
                private String name;
                
                public User() {}
            }
            """.trimIndent()

        val file = createJavaFile(tempDir = tempDir, className = "User", content = dirtyCode)
        val sanitizer = JavaSanitizer(runtimeOptions = RuntimeOptions(debug = false), log = logger)

        val result = sanitizer.sanitize(file = file)

        // verify that the sanitization process was marked as successful
        assertTrue(result.isSanitized)

        // verify that unwanted imports are removed, but standard imports are preserved
        assertFalse(result.code.contains("import com.fasterxml.jackson"))
        assertFalse(result.code.contains("import io.swagger"))
        assertFalse(result.code.contains("import jakarta.annotation"))
        assertContains(result.code, "import java.util.Objects;")
        assertContains(result.code, "import java.util.List;")

        // verify that unwanted annotations are removed
        assertFalse(result.code.contains("@Generated"))
        assertFalse(result.code.contains("@JsonInclude"))
        assertFalse(result.code.contains("@Schema"))
        assertFalse(result.code.contains("@JsonProperty"))

        // verify that internal constant fields are removed
        assertFalse(result.code.contains("JSON_PROPERTY_ID"))
        assertFalse(result.code.contains("JSON_PROPERTY_NAME"))

        // verify the class itself and its actual properties are still there
        assertContains(result.code, "public class User")
        assertContains(result.code, "private String id;")
        assertContains(result.code, "private String name;")
    }

    @Test
    fun `should preserve allowed annotations`(
        @TempDir tempDir: File,
    ) {
        val cleanCode =
            """
            package com.example.models;
            
            @Deprecated
            @SuppressWarnings("unchecked")
            public class OldUser {
                @Override
                public String toString() {
                    return "OldUser";
                }
            }
            """.trimIndent()

        val file = createJavaFile(tempDir = tempDir, className = "OldUser", content = cleanCode)
        val sanitizer = JavaSanitizer(runtimeOptions = RuntimeOptions(debug = false), log = logger)

        val result = sanitizer.sanitize(file = file)

        // verify that the sanitization process was marked as successful and that allowed annotations are preserved
        assertTrue(result.isSanitized)
        assertContains(result.code, "@Deprecated")
        assertContains(result.code, "@SuppressWarnings(\"unchecked\")")
        assertContains(result.code, "@Override")
    }

    @Test
    fun `should handle invalid java syntax gracefully and return original code`(
        @TempDir tempDir: File,
    ) {
        val brokenCode =
            """
            package com.example.models;
            
            public class BrokenModel {
                // Syntaktiskt fel här:
                private String name = ; 
            }
            """.trimIndent()

        val file = createJavaFile(tempDir = tempDir, className = "BrokenModel", content = brokenCode)
        val sanitizer = JavaSanitizer(runtimeOptions = RuntimeOptions(debug = false), log = logger)

        val result = sanitizer.sanitize(file = file)

        // the code was broken so no sanitation should have been done, and the original code should be returned
        assertFalse(result.isSanitized)
        assertEquals(brokenCode, result.code)

        // verify that we logged a warning and hinted about --debug
        assertEquals(1, capturedLogs.size)
        assertContains(capturedLogs.first(), "⚠️ Warning: OpenAPI Generator generated invalid Java syntax")
        assertContains(capturedLogs.first(), "(run with --debug for details).")
    }

    @Test
    fun `should log detailed java parser errors when debug is true on invalid syntax`(
        @TempDir tempDir: File,
    ) {
        val brokenCode = "public class BrokenModel { int x = ; }"
        val file = createJavaFile(tempDir = tempDir, className = "BrokenModel", content = brokenCode)
        val sanitizer = JavaSanitizer(runtimeOptions = RuntimeOptions(debug = true), log = logger)

        sanitizer.sanitize(file = file)

        // verify that detailed parsing information was logged, including line numbers and error messages
        assertTrue(capturedLogs.size >= 2)
        assertContains(capturedLogs[0], "Skipping sanitization.")
        assertContains(capturedLogs[1], "-> Reason:")
        assertContains(capturedLogs[1], "[Line:")
    }

    private fun createJavaFile(
        tempDir: File,
        className: String,
        content: String,
    ): File {
        val file = File(tempDir, "$className.java")
        file.writeText(content)
        return file
    }
}
