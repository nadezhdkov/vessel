import java.io.File

// Standalone Gradle project measuring DI container startup time. See
// docs/vessel-core.md for how Container.scan()/resolveAll() work.
// Deliberately NOT part of the main multi-module build: never listed in the
// root settings.gradle.kts, never touched by `./gradlew build` from the
// repo root. It reaches vessel-core only through a Gradle composite build
// (`includeBuild` in settings.gradle.kts), so it never becomes a real
// dependency of the main build in either direction.

plugins {
    id("application")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation("io.github.nadezhdkov:vessel-core:0.1.0")
}

application {
    mainClass.set("io.vessel.benchmark.VesselStartupBenchmark")
}

val beanCount: Int = (project.findProperty("beanCount") as String?)?.toInt() ?: 500

val generatedSourcesDir = layout.buildDirectory.dir("generated/sources/benchmark/generated")

// N distinct real classes, generated at build time (never committed) — the
// Container keys one BeanDefinition per Class, so "N beans" only means
// something if there really are N distinct types to register; N calls
// against one class would just overwrite the same registration N times.
val generateBeans by tasks.registering {
    inputs.property("beanCount", beanCount)
    outputs.dir(generatedSourcesDir)
    doLast {
        val dir = generatedSourcesDir.get().asFile
        // clear stale files from a previous run with a different beanCount —
        // otherwise shrinking N leaves extra generated classes behind.
        dir.deleteRecursively()
        dir.mkdirs()
        repeat(beanCount) { i ->
            val name = "Bean%04d".format(i)
            File(dir, "$name.java").writeText(
                """
                package generated;

                import io.vessel.core.annotation.Component;

                @Component
                public class $name {
                    public $name() {
                    }
                }
                """.trimIndent() + "\n"
            )
        }
    }
}

sourceSets {
    main {
        java.srcDir(generatedSourcesDir)
    }
}

tasks.named("compileJava") {
    dependsOn(generateBeans)
}
