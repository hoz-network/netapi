package net.hoz.netapi.client.module;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;
import net.hoz.api.commons.GameType;
import net.hoz.api.data.DataOperation;
import net.hoz.netapi.client.service.CurrencyDataProvider;
import net.hoz.netapi.client.service.GameDataProvider;
import net.hoz.netapi.client.service.NetLangService;
import net.hoz.netapi.client.service.PlayerDataProvider;

@RequiredArgsConstructor
public class ClientModule extends AbstractModule {
    private final GameType gameType;
    private final DataOperation.OriginSource originSource;

    @Override
    protected void configure() {
        //todo: do this from env

        install(new SinksModule());

        bind(GameType.class).toInstance(gameType);
        bind(DataOperation.OriginSource.class).toInstance(originSource);

        //TODO: rsocket configuration

        bind(CurrencyDataProvider.class).asEagerSingleton();
        bind(PlayerDataProvider.class).asEagerSingleton();
        bind(NetLangService.class).asEagerSingleton();

        if (originSource == DataOperation.OriginSource.GAME_SERVER) {
            bind(GameDataProvider.class).asEagerSingleton();
        }
    }
}
