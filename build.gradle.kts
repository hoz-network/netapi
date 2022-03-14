plugins {
    kotlin("jvm") version "1.6.20-M1"

    id("org.screamingsandals.plugin-builder") version "1.0.76"
    id("nebula.release") version "16.0.0"
}

allprojects {
    group = "net.hoz.netapi"

    repositories {
        mavenCentral()
        maven("https://repo.hoznet.dev/public")
        maven("https://repo.screamingsandals.org/public")
    }
}

subprojects {
    apply {
        plugin("java-library")
        plugin("idea")
        plugin("org.jetbrains.kotlin.jvm")
        plugin("org.screamingsandals.plugin-builder")
    }

    configurations.all {
        resolutionStrategy.cacheDynamicVersionsFor(0, "seconds")
    }
}