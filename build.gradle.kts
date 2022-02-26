plugins {
    kotlin("jvm") version "1.6.20-M1" apply false

    id("org.screamingsandals.plugin-builder") version "1.0.76"
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
        maven(url = "https://repo.hoznet.dev/snapshots")
        maven(url = "https://repo.screamingsandals.org/public")
    }
}