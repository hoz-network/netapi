pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        maven(url = "https://repo.screamingsandals.org/public/")
    }
}

rootProject.name = "netapi"

include("client")
include("api")
