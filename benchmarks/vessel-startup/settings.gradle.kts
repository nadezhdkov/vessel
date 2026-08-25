rootProject.name = "vessel-startup-benchmark"

includeBuild("../..") {
    dependencySubstitution {
        substitute(module("io.github.nadezhdkov:vessel-core")).using(project(":vessel-core"))
    }
}
