import net.hoz.netapi.Versions

dependencies {
    api(project(":api"))

    api(kotlin("reflect"))
    api("org.jetbrains.kotlinx", "kotlinx-coroutines-core", Versions.COROUTINES)

    api("com.google.guava", "guava", Versions.GUAVA)

    api("net.hoz", "netproto", Versions.NETPROTO)
    api("com.iamceph.resulter", "kotlin-extensions", Versions.RESULTER)

    api("org.screamingsandals.lib", "core-common", Versions.SANDALS)
    api("org.screamingsandals.lib", "lang", Versions.SANDALS)
    api("org.screamingsandals.lib", "kotlin-extra", Versions.SANDALS)

    api("com.google.inject", "guice", Versions.GUICE)
    api("com.github.ben-manes.caffeine", "caffeine", Versions.CAFFEINE)
    api("network.hoz", "kaffeine", Versions.KAFFEINE)

    api("org.apache.commons", "commons-lang3",Versions.COMMONS_LANG)
}
