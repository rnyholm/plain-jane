# Plain Jane

**Plain Jane** is a Java code generator for people who are tired of "Enterprise Magic".

## 👱‍♀️ Why "Plain Jane"?
We've all been there. You just want a simple Java class to hold some API data. You run a standard generator, and suddenly your project is bleeding annotations and depends on:
* `javax.annotation.*`
* `io.swagger.core.*`
* `org.apache.commons.*`
* The alignment of the stars ✨

**PlainJane** is different. She's not high maintenance. She doesn't need expensive frameworks or 50MB of transitive dependencies just to say "Hello". She gives you:
* **Plain Old Java Objects**.
* **Zero extra libraries**.
* **Clean, readable code**.

She's the girl next door of code generation. Simple, reliable, and absolutely drama-free.

## 🚀 Features
* **Lightning Fast:** Compiled ahead-of-time (AOT) into a native machine binary using GraalVM. Starts in milliseconds.
* **100% Standalone:** You don't even need Java installed on your machine to run her.
* **Smart Sanitization:** Uses AST-parsing (JavaParser) to clean up messy OpenAPI models and rip out useless annotations.
* **Flexible Input:** Feed her a local file (`api.yaml`) or a remote URL (`https://.../openapi.json`).
* **Safe & Sound:** Fail-fast validation ensures you don't start a build with missing arguments.
* **Scaffolding Ready:** Can generate a fully configured Maven project (`pom.xml`, `README.md`, `.gitignore`) alongside your models.
* **Beautiful UI:** A crisp, developer-friendly CLI experience with clear feedback.

## 🛠️ Building
Plain Jane runs on the bleeding edge of **Kotlin (2.3.0)**, but compiles down to a blazingly fast Native Image targeting **Java 21**.

To build her from source, you need **GraalVM JDK 21** installed (and C++ build tools if you are on Windows).

```bash
# Linux/Mac
./gradlew nativeCompile

# Windows (run from "x64 Native Tools Command Prompt for VS or similar")
.\gradlew.bat nativeCompile
```

**Artifact location:** 
* Linux/Mac: `build/native/nativeCompile/plain-jane`
* Windows: `build\native\nativeCompile\plain-jane.exe`

## 💻 Usage
Because Plain Jane is a native executable, you just run her directly. No JVM, no hassle.

```bash
# Linux/Mac
./plain-jane -i <input> -o <dir> -p <package> [options]

# Windows
.\plain-jane.exe -i <input> -o <dir> -p <package> [options]
```

### Core Options (Required)
| Flag              | Description                                                | Validation Rule              |
|-------------------|------------------------------------------------------------|------------------------------|
| `-i`, `--input`   | Path to local file or URL to the OpenAPI specification     | Must exist (if local file)   |
| `-o`, `--output`  | Path to the output directory where generated code is saved | Must be a directory path     |
| `-p`, `--package` | The Java package name (e.g. `com.example.models`)          | Standard Java package format |

### Maven Configuration
| Flag                        | Description                             | Default/Requirement                      |
|-----------------------------|-----------------------------------------|------------------------------------------|
| `-g`, `--group-id`          | Maven group ID                          | **Default:** Falls back to `<package>`   |
| `-a`, `--artifact-id`       | Maven artifact ID                       | **Required:** If generating a POM        |
| `-av`, `--artifact-version` | Version of the generated Maven artifact | **Default:** `1.0.0-SNAPSHOT`            |
| `--dist-id`                 | Distribution repository ID              | **Required:** If using distribution URLs | 
| `--dist-name`               | Distribution repository name            | Optional                                 |
| `--dist-url`                | Distribution repository URL             | **Required:** If using distribution ID   |

### Extras & Scaffolding
| Flag               | Description                                                             |
|--------------------|-------------------------------------------------------------------------|
| `-s`, `--scaffold` | Generate Maven project structure (`pom.xml`, `README.md` & `.gitignore` |
| `--with-pom`       | Generate `pom.xml` only                                                 |
| `--with-readme`    | Generate `README.md` only                                               |
| `--with-gitignore` | Generate `.gitignore` only                                              |

### Runtime Behavior
| Flag            | Description                                                                        |
|-----------------|------------------------------------------------------------------------------------|
| `-c`, `--clean` | Wipe the output directory completely before generating new files                   |
| `-d`, `--debug` | Enable debug logging to see underlying OpenAPI engine output and full stack traces |
| `--version`     | Show the version and exit                                                          |
| `-h`, `--help`  | Show the help message and exit                                                     |

## ⚙️ Development
Under the hood, Plain Jane utilizes **Kotlin**, **Clikt** for the beautiful terminal UI, **OpenAPI Generator** for raw code generation, and **JavaParser** to sanitize the AST and strip out the nonsense.

### Run in Dev Mode
During development, you don't need to wait for AOT compilation. You can run her directly on the JVM using Gradle:

```bash
# Linux/Mac
./gradlew run --args="-i api.yaml -o build/generated-models -p com.example.api -a my-api-client --scaffold"

# Windows
.\gradlew.bat run --args="-i api.yaml -o build/generated-models -p com.example.api -a my-api-client --scaffold"
```

## ⚠️ Note on JSON Serialization
Because Plain Jane generates pure Java without framework-specific annotations, ensure the consumer of the generated library configures their JSON mapper correctly (e.g., registering the `JavaTimeModule` in Jackson for `LocalDate`/`OffsetDateTime` support).
