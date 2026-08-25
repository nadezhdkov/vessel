import java.io.File

// Standalone Gradle project — see README.md's "Benchmark" section.
// The ONLY place in this repository that depends on Spring — deliberately
// never listed in the root settings.gradle.kts, never touched by
// `./gradlew build` from the repo root. Exists solely to give
// VesselStartupBenchmark (the sibling project) a real, equivalent number to
// compare against.

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
    implementation("org.springframework:spring-context:6.1.14")
}

application {
    mainClass.set("io.vessel.benchmark.spring.SpringStartupBenchmark")
}

val beanCount: Int = (project.findProperty("beanCount") as String?)?.toInt() ?: 500

val generatedSourcesDir = layout.buildDirectory.dir("generated/sources/benchmark/generated")

// Mirrors the sibling vessel-startup project's generateBeans task exactly —
// same N, same "one real class per bean" approach — so the two numbers are
// actually comparable rather than measuring two different things.
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

                import org.springframework.stereotype.Component;

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
