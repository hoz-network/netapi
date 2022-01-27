pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://repo.screamingsandals.org/public/")
    }
}

rootProject.name = "kneatpi"

include("client")
include("api")
