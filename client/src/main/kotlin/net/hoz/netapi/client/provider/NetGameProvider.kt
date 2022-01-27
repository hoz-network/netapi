package net.hoz.netapi.client.provider

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.StringValue
import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.core.model.ResultableData
import com.iamceph.resulter.kotlin.ifOk
import com.iamceph.resulter.kotlin.resultable
import com.iamceph.resulter.kotlin.unpack
import net.hoz.api.data.WUUID
import net.hoz.api.data.game.GameConfig
import net.hoz.api.data.game.ProtoGameFrame
import net.hoz.api.data.game.ProtoSpawnerType
import net.hoz.api.data.game.StoreHolder
import net.hoz.api.service.MGameType
import net.hoz.api.service.NetGameServiceClient
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.ReactorHelper
import net.hoz.netapi.api.onErrorHandle
import net.hoz.netapi.client.config.DataConfig
import org.slf4j.LoggerFactory
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import javax.inject.Inject

class NetGameProvider(
    private val gameService: NetGameServiceClient,
    private val clientConfig: DataConfig,
    private val gameTypeMessage: MGameType
) : Controlled {
    private val log = LoggerFactory.getLogger(javaClass)

    /**
     * Stores name-to-uuid values for games.
     */
    private val gameNameToUUID: MutableMap<String, UUID> = mutableMapOf()

    private val gameCache = Caffeine.newBuilder()
        .build<UUID, ProtoGameFrame>()

    private val configCache = Caffeine.newBuilder()
        .build<String, GameConfig>()

    private val storeCache = Caffeine.newBuilder()
        .build<String, StoreHolder>()

    private val spawnerCache = Caffeine.newBuilder()
        .build<String, ProtoSpawnerType>()

    @Inject
    constructor(gameService: NetGameServiceClient, clientConfig: DataConfig) : this(
        gameService,
        clientConfig,
        MGameType.newBuilder()
            .setType(clientConfig.gameType)
            .build()
    )

    override fun initialize() {
        subscribeForUpdates()
        createDataCache()
    }

    override fun dispose() {
        TODO("Not yet implemented")
    }

    /**
     * Tries to get one game with given ID from cache.
     *
     * @param gameId id of the game
     * @return [DataResultable] with result
     */
    fun oneGame(gameId: UUID): DataResultable<ProtoGameFrame> {
        return DataResultable.failIfNull(gameCache.getIfPresent(gameId), "Game not found.")
    }

    /**
     * Tries to get one game with given name from cache.
     *
     * @param name name of the game
     * @return [DataResultable] with result
     */
    fun oneGame(name: String): DataResultable<ProtoGameFrame> {
        val uuid = gameNameToUUID[name] ?: return DataResultable.fail("Game not found.")
        return oneGame(uuid)
    }

    /**
     * Gets all available games from cache.
     *
     * @return Collection of games.
     */
    fun allGames(): Collection<ProtoGameFrame> {
        return gameCache.asMap().values
    }

    /**
     * Tries to get one config with given name from cache.
     *
     * @param name name of the config
     * @return [DataResultable] with result
     */
    fun oneConfig(name: String): DataResultable<GameConfig> {
        return DataResultable.failIfNull(configCache.getIfPresent(name), "Config not found.")
    }

    /**
     * Gets all available configs from cache.
     *
     * @return Collection of configs.
     */
    fun allConfigs(): Collection<GameConfig> {
        return configCache.asMap().values
    }

    /**
     * Tries to get one store with given name from cache.
     *
     * @param name name of the store
     * @return [DataResultable] with result
     */
    fun oneStore(name: String): DataResultable<StoreHolder> {
        return DataResultable.failIfNull(storeCache.getIfPresent(name), "Store not found.")
    }

    /**
     * Gets all available stores from cache.
     *
     * @return Collection of stores.
     */
    fun allStores(): Collection<StoreHolder> {
        return storeCache.asMap().values
    }

    /**
     * Tries to get one spawner with given name from cache.
     *
     * @param name name of the spawner
     * @return [DataResultable] with result
     */
    fun oneSpawner(name: String): DataResultable<ProtoSpawnerType> {
        return DataResultable.failIfNull(spawnerCache.getIfPresent(name), "Spawner not found.")
    }

    /**
     * Gets all available spawners from cache.
     *
     * @return Collection of spawners.
     */
    fun allSpawners(): Collection<ProtoSpawnerType> {
        return spawnerCache.asMap().values
    }

    /**
     * Tries to retrieve the game from BAGR backend.
     *
     * @param gameId game id
     * @return [DataResultable] of the operation.
     */
    fun loadGame(gameId: UUID): Mono<DataResultable<ProtoGameFrame>> {
        return doGameLoading(
            gameService.oneById(
                WUUID.newBuilder()
                    .setValue(gameId.toString())
                    .build()
            )
        )
    }

    /**
     * Tries to retrieve the game from backend.
     *
     * @param name name of the game
     * @return [DataResultable] of the operation.
     */
    fun loadGame(name: String): Mono<DataResultable<ProtoGameFrame>> {
        return doGameLoading(
            gameService.oneByName(
                StringValue.newBuilder()
                    .setValue(name)
                    .build()
            )
        )
    }

    /**
     * Tries to save given game to the backend and replaces it in the cache.
     *
     * @param frame game to save.
     * @return [DataResultable] result of this operation.
     */
    fun saveGame(frame: ProtoGameFrame): Mono<DataResultable<UUID>> {
        //TODO: check this
        return gameService.saveGame(frame)
            .map { DataResultable.from(it.result, UUID.fromString(frame.uuid)) }
            .ifOk { gameCache.put(it, frame) }
            .onErrorHandle(log)
    }

    /**
     * Tries to save given spawner to the backend and caches it.
     *
     * @param spawnerTypeHolder holder to save
     * @return [Resultable] result of this operation.
     */
    fun saveSpawnerType(spawnerTypeHolder: ProtoSpawnerType): Mono<Resultable> {
        val spawnerName = spawnerTypeHolder.name

        return gameService.saveSpawnerType(spawnerTypeHolder)
            .resultable()
            .ifOk {
                spawnerCache.put(spawnerName, spawnerTypeHolder)
                log.debug("Saved new spawner[$spawnerName] for GameType[$gameTypeMessage].")
            }
            .onErrorHandle(log)
    }

    /**
     * Retrieves all available games from backend.
     *
     * @return Flux of [ProtoGameFrame]
     */
    fun loadGames(): Flux<ProtoGameFrame> {
        return gameService.all(gameTypeMessage)
            .doOnNext {
                log.trace("Received game [$it.name] - [$it.uuid] for GameType[$it.type].")
            }
            .onErrorHandle(log)
    }

    /**
     * Retrieves all available configs from backend.
     *
     * @return Flux of [GameConfig]
     */
    fun loadConfigs(): Flux<GameConfig> {
        return gameService.allConfigs(gameTypeMessage)
            .doOnNext {
                log.trace("Received config [$it.name] - [$it.uuid] for GameType[$it.type].")
            }
            .onErrorHandle(log)
    }

    /**
     * Retrieves all available stores from backend.
     *
     * @return Flux of [StoreHolder]
     */
    fun loadStores(): Flux<StoreHolder> {
        return gameService.allStores(gameTypeMessage)
            .doOnNext {
                log.trace("Received store holder [$it.name] - [$it.uuid] for GameType[$it.type].")
            }
            .onErrorHandle(log)
    }

    /**
     * Retrieves all available spawners from backend.
     *
     * @return Flux of [ProtoSpawnerType]
     */
    fun loadSpawners(): Flux<ProtoSpawnerType> {
        return gameService.allSpawnerTypes(gameTypeMessage)
            .doOnNext {
                log.trace("Received spawner [$it.name] - [$it.uuid] for GameType[$it.type].")
            }
            .onErrorHandle(log)
    }

    /**
     * Processes the given [ResultableData] into a [ProtoGameFrame].
     *
     * @param loadingMono mono from the backend
     * @return mono with [DataResultable] result of the operation.
     */
    private fun doGameLoading(loadingMono: Mono<ResultableData>): Mono<DataResultable<ProtoGameFrame>> {
        return loadingMono
            .unpack(ProtoGameFrame::class)
            .ifOk {
                val uuid = UUID.fromString(it.uuid)
                log.debug("Received game [$it.name] - [$uuid] for GameType[$it.type]")
                gameCache.put(uuid, it)
            }
            .onErrorHandle(log)
    }

    /**
     * Loads all cacheable values from the backend.
     * The order shouldn't be important at all.
     */
    private fun createDataCache() {
        loadGames()
            .doOnNext {
                log.trace("Caching new game[{}]...", it.name)
                val gameId = UUID.fromString(it.uuid)
                gameCache.put(gameId, it)

                gameNameToUUID[it.name] = gameId
            }
            .onErrorHandle(log)
            .subscribe()

        loadConfigs()
            .doOnNext { next: GameConfig ->
                log.trace("Caching new config[{}]...", next.name)
                configCache.put(next.name, next)
            }
            .onErrorHandle(log)
            .subscribe()

        loadStores()
            .doOnNext { next: StoreHolder ->
                log.trace("Caching new store[{}]...", next.name)
                storeCache.put(next.name, next)
            }
            .onErrorHandle(log)
            .subscribe()

        loadSpawners()
            .doOnNext { next: ProtoSpawnerType ->
                log.trace("Caching new spawner type[{}]...", next.name)
                spawnerCache.put(next.name, next)
            }
            .onErrorHandle(log)
            .subscribe()
    }

    private fun subscribeForUpdates() {}
}