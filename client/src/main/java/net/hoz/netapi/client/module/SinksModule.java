package net.hoz.netapi.client.module;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import net.hoz.api.data.game.GameData;
import net.hoz.api.data.game.NetGame;
import net.hoz.api.service.LangData;
import reactor.core.publisher.Sinks;

class SinksModule extends AbstractModule {

    @Override
    protected void configure() {
        //LanguageDataContainer updates Sink
        bind(new TypeLiteral<Sinks.Many<LangData>>() {
        }).toInstance(Sinks.many().multicast().directBestEffort());

        //GameFrameData updates Sink
        bind(new TypeLiteral<Sinks.Many<NetGame>>() {
        }).toInstance(Sinks.many().multicast().directBestEffort());

        //GameDataContainer updates Sink
        bind(new TypeLiteral<Sinks.Many<GameData>>() {
        }).toInstance(Sinks.many().multicast().directBestEffort());
    }
}
