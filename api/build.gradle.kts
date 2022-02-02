plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly("io.projectreactor", "reactor-core", "3.4.12")

    api("org.slf4j","slf4j-api", "1.7.32")
}