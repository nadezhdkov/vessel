plugins {
    id("java-library")
}

description = "Vessel web layer — controllers, dispatcher, argument resolver, JSON serializer (M6). Depends on vessel-core + vessel-http."

dependencies {
    api(project(":vessel-core"))
    api(project(":vessel-http"))
}
