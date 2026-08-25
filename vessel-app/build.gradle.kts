plugins {
    id("java-library")
}

description = "Vessel bootstrap — Vessel.start(), @Application; orchestrates DI + HTTP + web + config end-to-end (M10)."

dependencies {
    api(project(":vessel-core"))
    api(project(":vessel-http"))
    api(project(":vessel-web"))
    api(project(":vessel-config"))
}
