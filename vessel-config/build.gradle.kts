plugins {
    id("java-library")
}

description = "Vessel configuration layer — properties, Environment, @Value (M9). Depends on vessel-core."

dependencies {
    api(project(":vessel-core"))
}
