import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.7.0"

    id("org.screamingsandals.plugin-builder") version "1.0.77"
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
        plugin("org.jetbrains.kotlin.jvm")
        plugin("java-library")
        plugin("idea")
        plugin("org.screamingsandals.plugin-builder")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
        }
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(17))
        }
    }
}