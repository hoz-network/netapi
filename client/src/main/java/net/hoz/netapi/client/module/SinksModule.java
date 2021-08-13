package net.hoz.netapi.client.module;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import net.hoz.api.data.currency.CurrencyContainer;
import net.hoz.api.data.storage.GameDataContainer;
import net.hoz.api.data.storage.GameFrameData;
import net.hoz.api.data.storage.LanguageDataContainer;
import reactor.core.publisher.Sinks;

class SinksModule extends AbstractModule {

    @Override
    protected void configure() {
        //LanguageDataContainer updates Sink
        bind(new TypeLiteral<Sinks.Many<LanguageDataContainer>>() {
        }).toInstance(Sinks.many().multicast().directBestEffort());

        //GameFrameData updates Sink
        bind(new TypeLiteral<Sinks.Many<GameFrameData>>() {
        }).toInstance(Sinks.many().multicast().directBestEffort());

        //GameDataContainer updates Sink
        bind(new TypeLiteral<Sinks.Many<GameDataContainer>>() {
        }).toInstance(Sinks.many().multicast().directBestEffort());

        //CurrencyContainer updates Sink
        bind(new TypeLiteral<Sinks.Many<CurrencyContainer>>() {
        }).toInstance(Sinks.many().multicast().directBestEffort());

    }
}
