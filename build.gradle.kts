plugins {
    kotlin("jvm") version "1.6.10" apply false

    id("org.screamingsandals.plugin-builder") version "1.0.67"
}

allprojects {
    group = "net.hoz.netapi"
    version = "1.0.0-SNAPSHOT"
}

subprojects {
    apply {
        plugin("java-library")
        plugin("idea")
        plugin("org.screamingsandals.plugin-builder")
    }

    repositories {
        mavenCentral()
        mavenLocal()
        maven(url = "https://repo.screamingsandals.org/public")
    }
}