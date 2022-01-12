package net.hoz.netapi.client.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.StringValue;
import com.iamceph.resulter.core.DataResultable;
import com.iamceph.resulter.core.Resultable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.Controlled;
import net.hoz.api.data.ResultableData;
import net.hoz.api.data.WUUID;
import net.hoz.api.data.game.GameConfig;
import net.hoz.api.data.game.ProtoGameFrame;
import net.hoz.api.data.game.ProtoSpawnerType;
import net.hoz.api.data.game.StoreHolder;
import net.hoz.api.service.MGameType;
import net.hoz.api.service.NetGameServiceClient;
import net.hoz.api.util.Packeto;
import net.hoz.api.util.ReactorHelper;
import net.hoz.netapi.client.config.DataConfig;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Provider for {@link ProtoGameFrame} and other game-related data.
 * <p>
 * This provider communicates with our BAGR backend heavily and when the provider is initialized,
 * it asks BAGR about all data related to given {@link net.hoz.api.data.GameType}.
 */
@Slf4j
public class NetGameProvider implements Controlled {
    /**
     * Stores name-to-uuid values for games.
     */
    private final Map<String, UUID> gameNameToUUID = new ConcurrentHashMap<>();
    /**
     * Cached games.
     */
    @Getter
    private final Cache<UUID, ProtoGameFrame> gameCache = Caffeine.newBuilder()
            .build();
    /**
     * Cached game configurations.
     */
    @Getter
    private final Cache<String, GameConfig> configCache = Caffeine.newBuilder()
            .build();
    /**
     * Cached game stores.
     */
    @Getter
    private final Cache<String, StoreHolder> storeCache = Caffeine.newBuilder()
            .build();
    /**
     * Cached game spawner types.
     */
    @Getter
    private final Cache<String, ProtoSpawnerType> spawnerCache = Caffeine.newBuilder()
            .build();
    /**
     * RSocket service for communicating with BAGR.
     */
    private final NetGameServiceClient gameService;
    /**
     * A {@link net.hoz.api.data.GameType} message for protobuf.
     */
    private final MGameType gameTypeMessage;

    /**
     * Main constructor.
     *
     * @param gameService  RSocket game client.
     * @param clientConfig simple configuration
     */
    @Inject
    public NetGameProvider(NetGameServiceClient gameService,
                           DataConfig clientConfig) {
        this.gameService = gameService;
        this.gameTypeMessage = MGameType.newBuilder()
                .setType(clientConfig.gameType())
                .build();
    }

    @Override
    public void initialize() {
        subscribeForUpdates();
        createDataCache();
    }

    @Override
    public void dispose() {
        //TODO: handle destroying too
    }

    /**
     * Tries to get one game with given ID from cache.
     *
     * @param gameId id of the game
     * @return {@link DataResultable} with result
     */
    public DataResultable<ProtoGameFrame> oneGame(UUID gameId) {
        return DataResultable.failIfNull(gameCache.getIfPresent(gameId), "Game not found.");
    }

    /**
     * Tries to get one game with given name from cache.
     *
     * @param name name of the game
     * @return {@link DataResultable} with result
     */
    public DataResultable<ProtoGameFrame> oneGame(String name) {
        final var uuid = gameNameToUUID.get(name);
        if (uuid == null) {
            return DataResultable.fail("Game not found.");
        }
        return oneGame(uuid);
    }

    /**
     * Gets all available games from cache.
     *
     * @return Collection of games.
     */
    public Collection<ProtoGameFrame> allGames() {
        return gameCache.asMap().values();
    }

    /**
     * Tries to get one config with given name from cache.
     *
     * @param name name of the config
     * @return {@link DataResultable} with result
     */
    public DataResultable<GameConfig> oneConfig(String name) {
        return DataResultable.failIfNull(configCache.getIfPresent(name), "Config not found.");
    }

    /**
     * Gets all available configs from cache.
     *
     * @return Collection of configs.
     */
    public Collection<GameConfig> allConfigs() {
        return configCache.asMap().values();
    }

    /**
     * Tries to get one store with given name from cache.
     *
     * @param name name of the store
     * @return {@link DataResultable} with result
     */
    public DataResultable<StoreHolder> oneStore(String name) {
        return DataResultable.failIfNull(storeCache.getIfPresent(name), "Store not found.");
    }

    /**
     * Gets all available stores from cache.
     *
     * @return Collection of stores.
     */
    public Collection<StoreHolder> allStores() {
        return storeCache.asMap().values();
    }

    /**
     * Tries to get one spawner with given name from cache.
     *
     * @param name name of the spawner
     * @return {@link DataResultable} with result
     */
    public DataResultable<ProtoSpawnerType> oneSpawner(String name) {
        return DataResultable.failIfNull(spawnerCache.getIfPresent(name), "Spawner not found.");
    }

    /**
     * Gets all available spawners from cache.
     *
     * @return Collection of spawners.
     */
    public Collection<ProtoSpawnerType> allSpawners() {
        return spawnerCache.asMap().values();
    }

    /**
     * Tries to retrieve the game from BAGR backend.
     *
     * @param gameId game id
     * @return {@link DataResultable} of the operation.
     */
    public Mono<DataResultable<ProtoGameFrame>> loadGame(UUID gameId) {
        return doGameLoading(gameService.oneById(
                WUUID.newBuilder()
                        .setValue(gameId.toString())
                        .build())
        );
    }

    /**
     * Tries to retrieve the game from backend.
     *
     * @param name name of the game
     * @return {@link DataResultable} of the operation.
     */
    public Mono<DataResultable<ProtoGameFrame>> loadGame(String name) {
        return doGameLoading(gameService.oneByName(
                StringValue.newBuilder()
                        .setValue(name)
                        .build())
        );
    }

    /**
     * Tries to save given game to the backend and replaces it in the cache.
     *
     * @param frame game to save.
     * @return {@link DataResultable} result of this operation.
     */
    public Mono<DataResultable<UUID>> saveGame(ProtoGameFrame frame) {
        //TODO: check this
        return gameService.saveGame(frame)
                .map(next -> DataResultable.from(next.getResult(), UUID.fromString(frame.getUuid())))
                .doOnNext(next -> next.ifOk(data -> gameCache.put(data, frame)))
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    /**
     * Tries to save given spawner to the backend and caches it.
     *
     * @param spawnerTypeHolder holder to save
     * @return {@link Resultable} result of this operation.
     */
    public Mono<Resultable> saveSpawnerType(ProtoSpawnerType spawnerTypeHolder) {
        final var spawnerName = spawnerTypeHolder.getName();
        return gameService.saveSpawnerType(spawnerTypeHolder)
                .map(Resultable::convert)
                .doOnNext(next -> {
                    if (next.isOk()) {
                        spawnerCache.put(spawnerName, spawnerTypeHolder);
                        log.debug("Saved new spawner[{}] for GameType[{}].", spawnerName, gameTypeMessage.getType());
                    }
                })
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    /**
     * Retrieves all available games from backend.
     *
     * @return Flux of {@link ProtoGameFrame}
     */
    public Flux<ProtoGameFrame> loadGames() {
        return gameService.all(gameTypeMessage)
                .doOnNext(next -> log.trace("Received game [{}] - [{}] for GameType[{}].", next.getName(), next.getUuid(), next.getType()))
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    /**
     * Retrieves all available configs from backend.
     *
     * @return Flux of {@link GameConfig}
     */
    public Flux<GameConfig> loadConfigs() {
        return gameService.allConfigs(gameTypeMessage)
                .doOnNext(next -> log.trace("Received config[{}] for GameType[{}].", next.getName(), next.getType()))
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    /**
     * Retrieves all available stores from backend.
     *
     * @return Flux of {@link StoreHolder}
     */
    public Flux<StoreHolder> loadStores() {
        return gameService.allStores(gameTypeMessage)
                .doOnNext(next -> log.trace("Received store[{}] for GameType[{}].", next.getName(), next.getGameType()))
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    /**
     * Retrieves all available spawners from backend.
     *
     * @return Flux of {@link ProtoSpawnerType}
     */
    public Flux<ProtoSpawnerType> loadSpawners() {
        return gameService.allSpawnerTypes(gameTypeMessage)
                .doOnNext(next -> log.trace("Received store[{}] for GameType[{}].", next.getName(), next.getType()))
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    /**
     * Processes the given {@link ResultableData} into a {@link ProtoGameFrame}.
     *
     * @param loadingMono mono from the backend
     * @return mono with {@link DataResultable} result of the operation.
     */
    protected Mono<DataResultable<ProtoGameFrame>> doGameLoading(Mono<ResultableData> loadingMono) {
        return loadingMono
                .filter(ReactorHelper.filterResult(log))
                .map(result -> Packeto.unpack(result.getData(), ProtoGameFrame.class))
                .doOnNext(next -> next.ifOk(game -> {
                    final var uuid = UUID.fromString(game.getUuid());
                    log.debug("Received game [{}] - [{}] for GameType[{}]", game.getName(), uuid, game.getType());
                    gameCache.put(uuid, game);
                }))
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    /**
     * Loads all cacheable values from the backend.
     * The order shouldn't be important at all.
     */
    protected void createDataCache() {
        loadGames()
                .doOnNext(next -> {
                    log.trace("Caching new game[{}]...", next.getName());

                    final var gameId = UUID.fromString(next.getUuid());
                    gameCache.put(gameId, next);
                    gameNameToUUID.put(next.getName(), gameId);
                })
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();

        loadConfigs()
                .doOnNext(next -> {
                    log.trace("Caching new config[{}]...", next.getName());
                    configCache.put(next.getName(), next);
                })
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();

        loadStores()
                .doOnNext(next -> {
                    log.trace("Caching new store[{}]...", next.getName());
                    storeCache.put(next.getName(), next);
                })
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();

        loadSpawners()
                .doOnNext(next -> {
                    log.trace("Caching new spawner type[{}]...", next.getName());
                    spawnerCache.put(next.getName(), next);
                })
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();
    }

    protected void subscribeForUpdates() {

    }
}
