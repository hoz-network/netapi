package net.hoz.netapi.client.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.iamceph.resulter.core.DataResultable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.data.*;

import net.hoz.api.data.game.*;
import net.hoz.api.service.GameServiceClient;
import net.hoz.api.service.MGameType;
import net.hoz.netapi.client.util.Unpacker;
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
public class NetGameManager implements Disposable {
    @Getter
    private final Cache<Pair<String, GameData.Type>, GameData> gameDataCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    @Getter
    private final Cache<UUID, NetGame> gameCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10))
            .build();

    private final GameServiceClient gameService;
    private final GameType gameType;
    @Getter
    private final Sinks.Many<NetGame> gameSink;
    @Getter
    private final Sinks.Many<GameData> gameDataSink;

    private final MGameType gameTypeMessage;

    private Disposable frameUpdatesListener;
    private Disposable containerUpdatesListener;

    @Inject
    public NetGameManager(GameServiceClient gameService,
                          GameType gameType,
                          Sinks.Many<NetGame> gameSink,
                          Sinks.Many<GameData> gameDataSink,
                          Controllable controllable) {
        this.gameType = gameType;
        this.gameSink = gameSink;
        this.gameDataSink = gameDataSink;
        this.gameService = gameService;
        this.gameTypeMessage = MGameType.newBuilder().setType(gameType).build();

        controllable.enable(this::subscribeForUpdates);
        controllable.preDisable(this::dispose);
    }

    @Override
    public void dispose() {
        if (frameUpdatesListener != null) {
            frameUpdatesListener.dispose();
        }
        if (containerUpdatesListener != null) {
            containerUpdatesListener.dispose();
        }

        gameSink.tryEmitComplete();
        gameDataSink.tryEmitComplete();

        gameCache.invalidateAll();
        gameDataCache.invalidateAll();
    }

    public Flux<NetGame> all() {
        return gameService.all(gameTypeMessage)
                .doOnNext(next -> log.debug("all GameFrameData request: {}", next.getResult().getStatus()))
                .filter(ReactorHelper.filterResult(log))
                .map(next -> Unpacker.unpackUnsafe(next.getData(), NetGame.class))
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<DataResultable<NetGame>> oneFor(UUID uuid) {
        return Mono.fromSupplier(() -> gameCache.getIfPresent(uuid))
                .map(DataResultable::ok)
                .switchIfEmpty(gameService.oneById(WUUID.newBuilder()
                                .setValue(uuid.toString())
                                .build())
                        .doOnNext(next -> log.debug("oneFor GameFrameData request: {}", next.getResult().getStatus()))
                        .filter(ReactorHelper.filterResult(log))
                        .map(result -> Unpacker.unpackSafe(result.getData(), NetGame.class))
                        .onErrorResume(ex -> ReactorHelper.monoError(ex, log))
                );
    }

    public Mono<DataResultable<UUID>> update(UUID uuid, String gameData, String gameConfig) {
        return null;
        //return gameService.saveData(NetGame.newBuilder()
        //                        .setUuid(uuid.toString())
        //                        .setGameType(gameType)
        //                        .setGameData(GameData.newBuilder()
        //                                .setGameType(gameType)
        //                                .setData(gameData)
        //                                .build())
        //                        .setConfigData(GameData.newBuilder()
        //                                .setGameType(gameType)
        //                                .setData(gameConfig)
        //                                .build())
        //                .build())
        //        .map(next -> Resultable.convert(next.getResult()).transform(UUID.fromString(next.getUuid())))
        //        .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<GameData> defaultDataIdentified(GameData.Type type, String identifier) {
        return Mono.fromSupplier(() -> gameDataCache.getIfPresent(Pair.of(identifier, type)))
                .switchIfEmpty(allDefaultData(type)
                        .filter(next -> next.getName().equals(identifier))
                        .next()
                );
    }

    public Flux<GameData> allDefaultData(GameData.Type type) {
        return null;
       // return gameService.allData(
       //                 MessageGameDataType.newBuilder()
       //                         .setType(type)
       //                         .build())
       //         .filter(ReactorHelper.filterResult(log))
       //         .map(DefaultDataResponse::getContainer);
    }

    public List<Identifiable> getAvailableIdentifiers(GameData.Type type) {
        return gameDataCache.asMap()
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

        Flux.fromStream(Arrays.stream(GameData.Type.values())
                        .filter(next -> next != GameData.Type.UNRECOGNIZED)
                )
                .flatMap(this::allDefaultData)
                .filter(ReactorHelper.filterResult(log))
                .doOnNext(data -> {
                    log.trace("Adding default data to cache, type[{}], identifier[{}]", data.getDataType().name(), data.getName());
                    gameDataCache.put(Pair.of(data.getName(), data.getDataType()), data);
                })
                .subscribe();

        frameUpdatesListener = gameService.subscribeForNetGameUpdate(gameTypeMessage)
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .doOnNext(update -> {
                    gameSink.tryEmitNext(update);
                    gameCache.put(UUID.fromString(update.getUuid()), update);
                })
                .subscribe();
        //containerUpdatesListener = gameService.subscribeForGameDataUpdate(GameUpdatesRequest.newBuilder()
        //                .setType(gameType)
        //                .build())
        //        .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
        //        .doOnNext(update -> {
        //            gameDataSink.tryEmitNext(update);
        //            gameDataCache.put(Pair.of(update.getIdentifier(), update.getDataType()), update);
        //        })
        //        .subscribe();
    }
}
