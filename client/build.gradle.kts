import net.hoz.netapi.Versions

dependencies {
    api(project(":api"))

    compileOnly("commons-lang", "commons-lang", "2.6")
    api("com.google.guava", "guava", "30.1-jre")

    api("net.hoz", "netproto", Versions.NETPROTO)
    api("com.iamceph.resulter", "kotlin-extensions", Versions.RESULTER)

    api("org.screamingsandals.lib", "core-common", Versions.SANDALS)
    api("org.screamingsandals.lib", "command-common", Versions.SANDALS)
    api("org.screamingsandals.lib", "lang", Versions.SANDALS)
    api("org.screamingsandals.lib", "kotlin-extra", Versions.SANDALS)

    api("com.google.inject", "guice", Versions.GUICE)
    api("com.github.ben-manes.caffeine", "caffeine", Versions.CAFFEINE)
    api("network.hoz", "kaffeine", Versions.KAFFEINE)
}
