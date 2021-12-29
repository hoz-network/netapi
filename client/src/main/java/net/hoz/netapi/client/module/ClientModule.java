package net.hoz.netapi.client.module;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import lombok.RequiredArgsConstructor;
import net.hoz.api.Controlled;
import net.hoz.api.ControlledService;
import net.hoz.api.data.DataOperation;
import net.hoz.api.data.GameType;
import net.hoz.netapi.client.config.DataConfig;
import net.hoz.netapi.client.service.NetGameProvider;
import net.hoz.netapi.client.service.NetLangProvider;
import net.hoz.netapi.client.service.NetPlayerProvider;

@RequiredArgsConstructor
public class ClientModule extends AbstractModule {
    private final DataConfig clientConfig;

    @Override
    protected void configure() {
        //todo: do this from env

        install(new SinksModule());
        install(new RSocketModule(clientConfig));


        bind(GameType.class).toInstance(clientConfig.gameType());
        bind(DataOperation.OriginSource.class).toInstance(clientConfig.origin());

        //TODO: rsocket configuration

        bind(NetPlayerProvider.class).asEagerSingleton();
        bind(NetLangProvider.class).asEagerSingleton();

        if (clientConfig.origin() == DataOperation.OriginSource.GAME_SERVER) {
            bind(NetGameProvider.class).asEagerSingleton();
        }
    }

    @Provides
    @Singleton
    ControlledService buildControlledService(Injector injector) {
        final var services = injector.getAllBindings()
                .keySet()
                .stream()
                .filter(next -> Controlled.class.isAssignableFrom(next.getTypeLiteral().getRawType()))
                .map(next -> (Controlled) injector.getInstance(next))
                .toList();

        return new ControlledService(services);
    }
}
