package net.hoz.netapi.client.module;

import com.google.inject.AbstractModule;
import lombok.RequiredArgsConstructor;
import net.hoz.api.commons.DataOperation;
import net.hoz.api.commons.GameType;
import net.hoz.netapi.client.grpc.CurrencyDataClient;
import net.hoz.netapi.client.grpc.GameDataClient;
import net.hoz.netapi.client.grpc.LanguageClient;
import net.hoz.netapi.client.grpc.PlayerDataClient;
import net.hoz.netapi.client.service.InjectableGrpcChannelService;
import net.hoz.netapi.client.service.InjectableGrpcStubService;
import net.hoz.netapi.grpc.config.GrpcConfig;
import net.hoz.netapi.grpc.service.GrpcChannelService;
import net.hoz.netapi.grpc.service.GrpcStubService;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
public class ClientModule extends AbstractModule {
    private final GameType gameType;
    private final DataOperation.OriginSource originSource;

    @Override
    protected void configure() {
        //todo: do this from env
        final var config = new GrpcConfig();
        config.setServiceId(UUID.randomUUID());
        config.setAddress(List.of("localhost:6565"));
        config.setToken(List.of("f2f72612-aaf3-4f0f-a989-b37ce5a528cd"));
        config.setActiveServers(1);
        config.setThreadsCount(8);
        config.setCheckTimeSeconds(20);

        install(new SinksModule());

        bind(GameType.class).toInstance(gameType);
        bind(DataOperation.OriginSource.class).toInstance(originSource);

        bind(GrpcConfig.class).toInstance(config);
        bind(GrpcChannelService.class).to(InjectableGrpcChannelService.class).asEagerSingleton();
        bind(GrpcStubService.class).to(InjectableGrpcStubService.class).asEagerSingleton();

        bind(CurrencyDataClient.class).asEagerSingleton();
        bind(PlayerDataClient.class).asEagerSingleton();
        bind(LanguageClient.class).asEagerSingleton();

        if (originSource == DataOperation.OriginSource.GAME_SERVER) {
            bind(GameDataClient.class).asEagerSingleton();
        }
    }
}
