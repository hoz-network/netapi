dependencies {
    api(project(":api"))

    compileOnly("commons-lang", "commons-lang", "2.6")
    api("com.google.guava", "guava", "30.1-jre")

    api("net.hoz", "netproto", "1.0.0-SNAPSHOT")
    api("com.iamceph.resulter", "kotlin-extensions", "1.1.6")

    //rsocket
    api("io.rsocket", "rsocket-core", "1.1.1")
    api("io.rsocket", "rsocket-transport-netty", "1.1.1")

    api("org.screamingsandals.lib", "core-common", "2.0.1-SNAPSHOT")
    api("org.screamingsandals.lib", "command-common", "2.0.1-SNAPSHOT")
    api("org.screamingsandals.lib", "lang", "2.0.1-SNAPSHOT")
    api("org.screamingsandals.lib", "kotlin-extra", "2.0.1-SNAPSHOT")

    api("com.google.inject", "guice", "5.0.1")
    api("com.github.ben-manes.caffeine", "caffeine", "3.0.3")
    api("network.hoz", "kaffeine", "1.0.0-SNAPSHOT")
}
