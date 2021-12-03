package net.hoz.netapi.client.module;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;
import net.hoz.api.data.DataOperation;
import net.hoz.api.data.game.GameType;
import net.hoz.netapi.client.service.NetGameManager;
import net.hoz.netapi.client.service.NetLangManager;
import net.hoz.netapi.client.service.NetPlayerManager;

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

        bind(NetPlayerManager.class).asEagerSingleton();
        bind(NetLangManager.class).asEagerSingleton();

        if (originSource == DataOperation.OriginSource.GAME_SERVER) {
            bind(NetGameManager.class).asEagerSingleton();
        }
    }
}
