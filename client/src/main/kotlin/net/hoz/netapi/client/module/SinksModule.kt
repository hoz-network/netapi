package net.hoz.netapi.client.module

import com.google.inject.AbstractModule
import com.google.inject.TypeLiteral
import net.hoz.api.data.game.ProtoGameFrame
import net.hoz.api.service.LangData
import reactor.core.publisher.Sinks
import reactor.core.publisher.Sinks.Many

class SinksModule : AbstractModule() {
    override fun configure() {
        bind(object : TypeLiteral<Many<LangData>>() {})
            .toInstance(Sinks.many().multicast().directBestEffort())

        bind(object : TypeLiteral<Many<ProtoGameFrame>>() {})
            .toInstance(Sinks.many().multicast().directBestEffort())
    }
}
