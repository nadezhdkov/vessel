plugins {
    id("application")
}

description = "Vessel example application — HelloApplication/User/UserService/UserController, compiled by the build and runnable via ./gradlew :vessel-examples:run."

dependencies {
    implementation(project(":vessel-app"))
}

application {
    mainClass.set("io.vessel.examples.HelloApplication")
}
