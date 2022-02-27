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
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.screamingsandals.plugin-builder")
    }

    repositories {
        mavenCentral()
        maven("https://repo.hoznet.dev/snapshots")
        maven("https://repo.screamingsandals.org/public")
    }
}