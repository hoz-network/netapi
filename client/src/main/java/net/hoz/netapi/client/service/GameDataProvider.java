package net.hoz.netapi.client.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.iamceph.resulter.core.DataResultable;
import com.iamceph.resulter.core.Resultable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.commons.GameType;
import net.hoz.api.commons.GameUpdatesRequest;
import net.hoz.api.data.Identifiable;
import net.hoz.api.data.ReactorHelper;
import net.hoz.api.data.storage.DataType;
import net.hoz.api.data.storage.GameDataContainer;
import net.hoz.api.data.storage.GameFrameData;
import net.hoz.api.service.game.*;
import org.screamingsandals.lib.utils.Controllable;
import org.screamingsandals.lib.utils.Pair;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
public class GameDataProvider implements Disposable {
    @Getter
    private final Cache<UUID, GameFrameData> gameFrameDataCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();
    @Getter
    private final Cache<Pair<String, DataType>, GameDataContainer> dataContainerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    private final GameType gameType;
    @Getter
    private final Sinks.Many<GameFrameData> frameUpdates;
    @Getter
    private final Sinks.Many<GameDataContainer> containerUpdates;

    private final GameDataServiceClient gameService;

    private Disposable frameUpdatesListener;
    private Disposable containerUpdatesListener;

    @Inject
    public GameDataProvider(GameType gameType,
                            Controllable controllable,
                            Sinks.Many<GameFrameData> frameUpdates,
                            Sinks.Many<GameDataContainer> containerUpdates,
                            GameDataServiceClient gameService) {
        this.gameType = gameType;
        this.frameUpdates = frameUpdates;
        this.containerUpdates = containerUpdates;
        this.gameService = gameService;

        controllable.disable(this::dispose);
        subscribeForUpdates();

        Flux.fromStream(Arrays.stream(DataType.values()).filter(next -> next != DataType.UNRECOGNIZED))
                .flatMap(this::allDefaultData)
                .doOnNext(data -> {
                    log.trace("Adding default data to cache, type[{}], identifier[{}]", data.getDataType().name(), data.getIdentifier());
                    dataContainerCache.put(Pair.of(data.getIdentifier(), data.getDataType()), data);
                })
                .subscribe();

    }

    @Override
    public void dispose() {
        if (frameUpdatesListener != null) {
            frameUpdatesListener.dispose();
        }
        if (containerUpdatesListener != null) {
            containerUpdatesListener.dispose();
        }

        frameUpdates.tryEmitComplete();
        containerUpdates.tryEmitComplete();

        gameFrameDataCache.invalidateAll();
        dataContainerCache.invalidateAll();
    }

    public Flux<GameFrameData> all() {
        return gameService.requestAllGameFrames(AllGameFrameDataRequest.newBuilder()
                        .setType(gameType)
                        .build())
                .doOnNext(next -> log.debug("all GameFrameData request: {}", next.getResult().getStatus()))
                .filter(ReactorHelper.filterResult(log))
                .map(GameFrameDataResponse::getData)
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<GameFrameData> oneFor(UUID uuid) {
        return Mono.fromSupplier(() -> gameFrameDataCache.getIfPresent(uuid))
                .switchIfEmpty(gameService.requestGameFrame(GameFrameDataRequest.newBuilder()
                                .setUuid(uuid.toString())
                                .setType(gameType)
                                .build())
                        .doOnNext(next -> log.debug("oneFor GameFrameData request: {}", next.getResult().getStatus()))
                        .filter(ReactorHelper.filterResult(log))
                        .map(GameFrameDataResponse::getData)
                        .onErrorResume(ex -> ReactorHelper.monoError(ex, log))
                );
    }

    public Mono<DataResultable<UUID>> update(UUID uuid, String gameData, String gameConfig) {
        return gameService.updateGameData(GameFrameDataSaveRequest.newBuilder()
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
                .map(next -> Resultable.convert(next.getResult()).transform(UUID.fromString(next.getUuid())))
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
        return gameService.requestDefaultData(
                        RequestDefaultData.newBuilder()
                                .setType(gameType)
                                .setDataType(type)
                                .build())
                .filter(ReactorHelper.filterResult(log))
                .map(DefaultDataResponse::getContainer);
    }

    public List<Identifiable> getAvailableIdentifiers(DataType type) {
        return dataContainerCache.asMap()
                .keySet()
                .stream()
                .filter(gameDataContainer -> gameDataContainer.getSecond() == type)
                .map(Pair::getFirst)
                .map(Identifiable::of)
                .collect(Collectors.toList());
    }

    private void subscribeForUpdates() {
        if (frameUpdatesListener != null) {
            frameUpdatesListener.dispose();
        }
        if (containerUpdatesListener != null) {
            containerUpdatesListener.dispose();
        }

        frameUpdatesListener = gameService.subscribeFrames(GameUpdatesRequest.newBuilder()
                        .setType(gameType)
                        .build())
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .doOnNext(update -> {
                    frameUpdates.tryEmitNext(update);
                    gameFrameDataCache.put(UUID.fromString(update.getUuid()), update);
                })
                .subscribe();
        containerUpdatesListener = gameService.subscribeContainers(GameUpdatesRequest.newBuilder()
                        .setType(gameType)
                        .build())
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .doOnNext(update -> {
                    containerUpdates.tryEmitNext(update);
                    dataContainerCache.put(Pair.of(update.getIdentifier(), update.getDataType()), update);
                })
                .subscribe();
    }
}
