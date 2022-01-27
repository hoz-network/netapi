plugins {
    kotlin("jvm")
}

dependencies {
    compileOnly("commons-lang", "commons-lang", "2.6")

    api(project(":api"))
    api("net.hoz", "netproto", "1.0.0-SNAPSHOT")
    api("com.iamceph.resulter", "kotlin-extensions", "1.1.4-SNAPSHOT")

    //rsocket
    api("io.rsocket", "rsocket-core", "1.1.1")
    api("io.rsocket", "rsocket-transport-netty", "1.1.1")

    api("org.screamingsandals.lib", "core-common", "2.0.1-SNAPSHOT")
    api("org.screamingsandals.lib", "utils-common", "2.0.1-SNAPSHOT")
    api("org.screamingsandals.lib", "command-common", "2.0.1-SNAPSHOT")
    api("org.screamingsandals.lib", "lang", "2.0.1-SNAPSHOT")

    api("com.google.inject", "guice", "5.0.1")
    api("com.github.ben-manes.caffeine", "caffeine", "3.0.3")
}