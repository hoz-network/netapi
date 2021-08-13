package net.hoz.netapi.client.grpc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.commons.GameType;
import net.hoz.api.commons.GameUpdatesRequest;
import net.hoz.api.data.storage.DataType;
import net.hoz.api.data.storage.GameDataContainer;
import net.hoz.api.data.storage.GameFrameData;
import net.hoz.api.result.DataResult;
import net.hoz.api.service.game.*;
import net.hoz.netapi.grpc.service.GrpcStubService;
import net.hoz.netapi.grpc.util.ReactorHelper;
import org.screamingsandals.lib.utils.Controllable;
import org.screamingsandals.lib.utils.Pair;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class GameDataClient implements Disposable {
    private final Cache<UUID, GameFrameData> gameFrameDataCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
    private final Cache<Pair<String, DataType>, GameDataContainer> dataContainerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    private final GameType gameType;
    @Getter
    private final Sinks.Many<GameFrameData> frameUpdates;
    @Getter
    private final Sinks.Many<GameDataContainer> containerUpdates;

    private final AtomicReference<ReactorGameDataProviderGrpc.ReactorGameDataProviderStub> stub;

    private Disposable frameUpdatesListener;
    private Disposable containerUpdatesListener;

    @Inject
    public GameDataClient(GameType gameType,
                          GrpcStubService stubService,
                          Controllable controllable,
                          Sinks.Many<GameFrameData> frameUpdates,
                          Sinks.Many<GameDataContainer> containerUpdates) {
        this.gameType = gameType;
        this.frameUpdates = frameUpdates;
        this.containerUpdates = containerUpdates;

        final var holder = stubService.getHolder(ReactorGameDataProviderGrpc.class);
        this.stub = holder.getStub(ReactorGameDataProviderGrpc.ReactorGameDataProviderStub.class);

        controllable.disable(this::dispose);
        controllable.postEnable(() -> {
            holder.getChannel().renewCallback(this::subscribeForUpdates);
            subscribeForUpdates();

            Flux.fromStream(Arrays.stream(DataType.values())
                            .filter(next -> next != DataType.UNRECOGNIZED))
                    .flatMap(this::allDefaultData)
                    .doOnNext(data -> {
                        log.trace("Adding default data to cache, type[{}], identifier[{}]", data.getDataType().name(), data.getIdentifier());
                        dataContainerCache.put(Pair.of(data.getIdentifier(), data.getDataType()), data);
                    });

            this.frameUpdates.asFlux()
                    .doOnNext(next -> gameFrameDataCache.put(UUID.fromString(next.getUuid()), next));
            this.containerUpdates.asFlux()
                    .doOnNext(data -> dataContainerCache.put(Pair.of(data.getIdentifier(), data.getDataType()), data));
        });
    }

    @Override
    public void dispose() {
        frameUpdatesListener.dispose();
        containerUpdatesListener.dispose();

        frameUpdates.tryEmitComplete();
        containerUpdates.tryEmitComplete();

        dataContainerCache.invalidateAll();
    }

    public Flux<GameFrameData> all() {
        return stub.get()
                .requestAllGameFrames(AllGameFrameDataRequest.newBuilder()
                        .setType(gameType)
                        .build())
                .doOnNext(next -> log.debug("all GameFrameData request: {}", next.getResult().getStatus()))
                .filter(next -> ReactorHelper.resultFilter("all", next.getResult(), log))
                .map(GameFrameDataResponse::getData)
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<GameFrameData> oneFor(UUID uuid) {
        return Mono.fromSupplier(() -> gameFrameDataCache.getIfPresent(uuid))
                .switchIfEmpty(stub.get()
                        .requestGameFrame(GameFrameDataRequest.newBuilder()
                                .setUuid(uuid.toString())
                                .setType(gameType)
                                .build())
                        .doOnNext(next -> log.debug("oneFor GameFrameData request: {}", next.getResult().getStatus()))
                        .filter(next -> ReactorHelper.resultFilter("oneFor", next.getResult(), log))
                        .map(GameFrameDataResponse::getData)
                        .onErrorResume(ex -> ReactorHelper.monoError(ex, log))
                );
    }

    public Mono<DataResult<UUID>> update(UUID uuid, String gameData, String gameConfig) {
        return stub.get()
                .updateGameData(GameFrameDataSaveRequest.newBuilder()
                        .setData(GameFrameData.newBuilder()
                                .setUuid(uuid.toString())
                                .setGameType(gameType)
                                .setGameData(GameDataContainer.newBuilder()
                                        .setGameType(gameType)
                                        .setData(gameData)
                                        .build())
                                .setConfigData(GameDataContainer.newBuilder()
                                        .setGameType(gameType)
                                        .setData(gameConfig)
                                        .build()))
                        .build())
                .map(next -> DataResult.convert(next.getResult(), UUID.fromString(next.getUuid())))
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<GameDataContainer> defaultDataIdentified(DataType type, String identifier) {
        return Mono.fromSupplier(() -> dataContainerCache.getIfPresent(Pair.of(identifier, type)))
                .switchIfEmpty(allDefaultData(type)
                        .filter(next -> next.getIdentifier().equals(identifier))
                        .next()
                );
    }

    public Flux<GameDataContainer> allDefaultData(DataType type) {
        return stub.get()
                .requestDefaultData(
                        RequestDefaultData.newBuilder()
                                .setType(gameType)
                                .setDataType(type)
                                .build())
                .filter(next -> ReactorHelper.resultFilter("allDefaultData", next.getResult(), log))
                .map(DefaultDataResponse::getContainer);
    }


    private void subscribeForUpdates() {
        if (frameUpdatesListener != null) {
            frameUpdatesListener.dispose();
        }
        if (containerUpdatesListener != null) {
            containerUpdatesListener.dispose();
        }

        frameUpdatesListener = stub.get()
                .listenForFrameUpdates(GameUpdatesRequest.newBuilder()
                        .setType(gameType)
                        .build())
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .doOnNext(frameUpdates::tryEmitNext)
                .subscribe();
        containerUpdatesListener = stub.get()
                .listenForDataUpdates(GameUpdatesRequest.newBuilder()
                        .setType(gameType)
                        .build())
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .doOnNext(containerUpdates::tryEmitNext)
                .subscribe();
    }
}
