package net.hoz.netapi.client.module

import com.google.inject.AbstractModule
import com.google.inject.Injector
import com.google.inject.Provides
import com.google.inject.Singleton
import net.hoz.api.data.DataOperation.OriginSource
import net.hoz.api.data.GameType
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.ControlledService
import net.hoz.netapi.client.config.DataConfig
import net.hoz.netapi.client.provider.NetGameProvider
import net.hoz.netapi.client.provider.NetLangProvider
import net.hoz.netapi.client.provider.NetPlayerProvider

data class ClientModule(private val config: DataConfig) : AbstractModule() {

    override fun configure() {
        install(SinksModule())
        install(RSocketModule(config));

        bind(GameType::class.java).toInstance(config.gameType)
        bind(OriginSource::class.java).toInstance(config.origin)


        //TODO: rsocket configuration
        bind(NetPlayerProvider::class.java).asEagerSingleton()
        bind(NetLangProvider::class.java).asEagerSingleton()

        if (config.origin == OriginSource.GAME_SERVER) {
            bind(NetGameProvider::class.java).asEagerSingleton()
        }
    }

    @Provides
    @Singleton
    fun buildControlledService(injector: Injector): ControlledService {
        val services = injector.allBindings.keys
            .filter { Controlled::class.java.isAssignableFrom(it.typeLiteral.rawType) }
            .map { injector.getInstance(it) as Controlled }
            .toList()

        return ControlledService(services)
    }
}
