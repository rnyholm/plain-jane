# Plain Jane

**Plain Jane** is a Java code generator for people who are tired of "Enterprise Magic".

## 👱‍♀️ Why "Plain Jane"?
We've all been there. You just want a simple Java class to hold some API data. You run a standard generator, and suddenly your project is bleeding annotations and depends on:
* `javax.annotation.*`
* `io.swagger.core.*`
* `com.fasterxml.jackson.*` (or `com.google.gson.*`)
* The alignment of the stars ✨

**PlainJane** is different. She's not high maintenance. She doesn't need expensive frameworks or 50MB of transitive dependencies just to say "Hello". She gives you:
* **Plain Old Java Objects**.
* **Zero extra libraries**.
* **Clean, readable code**.

She's the girl next door of code generation. Simple, reliable, and absolutely drama-free.

## 🚀 Features

* **Minimalist:** The generated code is 100% standalone. No runtime dependencies required.
* **Smart Sanitization:** Uses AST-parsing (JavaParser) to clean up messy OpenAPI models, rip out useless annotations, and automatically patch syntax bugs from the underlying generation engine.
* **Flexible Input:** Feed her a local file (`api.yaml`) or a remote URL (`https://.../openapi.json`).
* **Safe & Sound:** Fail-fast validation ensures you don't start a build with missing arguments.
* **Scaffolding Ready:** Can generate a fully configured Maven project (`pom.xml`, `README.md`, `.gitignore`) alongside your models.
* **Beautiful UI:** A crisp, developer-friendly CLI experience with clear feedback.

## 🛠️ Building
Plain Jane is built on a modern stack, leveraging **Kotlin 2.3.0** and **Java 21**.

Build the executable shadow jar (Plain Jane doesn't travel light herself; she carries her own dependencies so your generated code doesn't have to):

```bash
# Linux/Mac
./gradlew shadowJar

# Windows
.\gradlew.bat shadowJar
```

**Artifact location:** `build/libs/plain-jane.jar`

## 💻 Usage

```bash
java -jar plain-jane.jar -i <input> -o <dir> -p <package> [options]
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

```bash
# Linux/Mac
./gradlew run --args="-i api.yaml -o build/generated-models -p com.example.api -a my-api-client --scaffold"

# Windows
.\gradlew.bat run --args="-i api.yaml -o build/generated-models -p com.example.api -a my-api-client --scaffold"
```

## ⚠️ Note on JSON Serialization & Polymorphism
Plain Jane's primary goal is to generate 100% pure Java code without framework-specific annotations (like `@JsonProperty` or `@JsonSubTypes`).

Because of this strict "Zero Dependencies" rule, she does not dictate how you serialize or deserialize your data.
* **Dates/Times:** Ensure your JSON mapper is configured to handle `java.time` objects (e.g., registering `JavaTimeModule` in Jackson).
* **Polymorphism (`oneOf`/`anyOf`):** Plain Jane generates the correct class hierarchies, but she strips out the engine's default static discriminator blocks to avoid forcing external library dependencies. You must configure your JSON mapper (e.g., via Jackson Mixins or Gson Runtime Adapters) to handle subtype mapping manually.

## ⚖️ Acknowledgments
**Test Data:** The automated stress-testing suite for Plain Jane utilizes the [Stripe OpenAPI Specification](https://github.com/stripe/openapi), which is graciously provided by Stripe, Inc. under the MIT License.
