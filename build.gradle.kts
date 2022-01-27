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

    dependencies {
        api("com.google.guava", "guava", "30.1-jre")
        api("org.slf4j","slf4j-api", "1.7.32")
    }
}