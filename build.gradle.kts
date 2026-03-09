plugins {
    kotlin("jvm") version "2.3.0"
    application
    id("com.gradleup.shadow") version "9.3.1"
}

group = "ax.stardust"
version = libs.versions.plainjane.get()

val generatedResourcesDir: Provider<Directory> = layout.buildDirectory.dir("generated/plainjane/resources")

val generateVersionProperties by tasks.registering {
    val propFile = generatedResourcesDir.get().file("version.properties").asFile
    outputs.file(propFile)

    doLast {
        propFile.parentFile.mkdirs()
        propFile.writeText(
            """
            plainjane.version=${libs.versions.plainjane.get()}
            openapi.generator.version=${libs.versions.openapi.generator.get()}
            """.trimIndent(),
        )
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.clikt)
    implementation(libs.openapi.generator)
    implementation(libs.javaparser)
    implementation(libs.caffeine) // force a newer version to avoid unsafe warnings on Java 17+
    implementation(libs.slf4j)

    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.kotlin.test)
}

application {
    mainClass.set("ax.stardust.plainjane.cli.PlainJaneCommandKt")
}

kotlin {
    jvmToolchain(21)
}

sourceSets {
    main {
        resources {
            srcDir(generatedResourcesDir)
        }
    }
}

tasks {
    withType<JavaExec> {
        jvmArgs =
            listOf(
                "-Dfile.encoding=UTF-8",
                "-Dsun.stdout.encoding=UTF-8",
                "-Dsun.stderr.encoding=UTF-8",
                "--enable-native-access=ALL-UNNAMED",
            )
    }
    shadowJar {
        archiveFileName.set("plain-jane.jar")

        manifest {
            attributes["Main-Class"] = "ax.stardust.plainjane.cli.PlainJaneCommandKt"
            attributes["Implementation-Version"] = project.version
            attributes["Description"] = "The no-nonsense OpenAPI generator"
            attributes["Enable-Native-Access"] = "ALL-UNNAMED"
        }
    }
    processResources {
        dependsOn(generateVersionProperties)
    }
    withType<Test> {
        useJUnitPlatform() // force JUnit 5
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}
