import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.plugins.signing.SigningExtension

plugins {
    id("java")
    // 0.34.0, not the newer 0.35.0+, deliberately — 0.35.0 raises the minimum
    // supported Gradle version to 8.13, and this project's wrapper is on
    // 8.10. 0.34.0's minimum is Gradle 8.5, well within range, and it
    // already dropped legacy OSSRH support in favor of the Central Portal
    // API this project needs.
    id("com.vanniktech.maven.publish") version "0.34.0" apply false
}

allprojects {
    group = "io.github.nadezhdkov"
    version = "0.1.0"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "java")

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(21))
        }
    }

    // The generated type-safe `libs.*` accessors aren't available inside an
    // allprojects{}/subprojects{} closure (a documented Gradle limitation —
    // confirmed by testing `libs.junit.jupiter` at the root script level,
    // which works fine, vs. inside this closure, which doesn't) — resolving
    // the catalog through this general extension API instead works the same
    // way from any project.
    val libs = rootProject.the<VersionCatalogsExtension>().named("libs")

    dependencies {
        testImplementation(platform(libs.findLibrary("junit-bom").get()))
        testImplementation(libs.findLibrary("junit-jupiter").get())
        testRuntimeOnly(libs.findLibrary("junit-platform-launcher").get())
    }

    tasks.test {
        useJUnitPlatform()
    }

    tasks.withType<JavaCompile> {
        // Needed since M6: @PathVariable/@RequestParam fall back to the real
        // parameter name (e.g. "id") when no explicit name is given, which
        // requires -parameters — otherwise reflection only sees "arg0".
        options.compilerArgs.add("-parameters")
    }

    // vessel-examples is a runnable demo app (application plugin), not a
    // library — nothing to publish. Every other module ships to Maven
    // Central under the same io.github.nadezhdkov coordinates the root
    // project already declares.
    if (name != "vessel-examples") {
        apply(plugin = "com.vanniktech.maven.publish")

        configure<MavenPublishBaseExtension> {
            publishToMavenCentral()
            signAllPublications()

            pom {
                name.set(project.name)
                description.set(project.description ?: "Vessel — a didactic Java 21 micro-framework.")
                inceptionYear.set("2026")
                url.set("https://github.com/nadezhdkov/vessel")
                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }
                developers {
                    developer {
                        id.set("nadezhdkov")
                        name.set("Rick M. Viana")
                        url.set("https://github.com/nadezhdkov/")
                    }
                }
                scm {
                    url.set("https://github.com/nadezhdkov/vessel")
                    connection.set("scm:git:git://github.com/nadezhdkov/vessel.git")
                    developerConnection.set("scm:git:ssh://git@github.com/nadezhdkov/vessel.git")
                }
            }
        }

        // Matches the signing.gnupg.keyName property already in
        // gradle.properties — GPG-agent-based signing, not an in-memory or
        // secretKeyRingFile key. signAllPublications() above only enables
        // signing; useGpgCmd() is what picks this specific method.
        configure<SigningExtension> {
            useGpgCmd()
        }
    }
}
