package net.hoz.netapi.client.provider

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.StringValue
import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.core.model.ResultableData
import com.iamceph.resulter.kotlin.ifOk
import com.iamceph.resulter.kotlin.resultable
import com.iamceph.resulter.kotlin.unpack
import mu.KotlinLogging
import net.hoz.api.data.WUUID
import net.hoz.api.data.game.GameConfig
import net.hoz.api.data.game.ProtoGameFrame
import net.hoz.api.data.game.ProtoSpawnerType
import net.hoz.api.data.game.StoreHolder
import net.hoz.api.service.MGameType
import net.hoz.api.service.NetGameServiceClient
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.onErrorHandle
import net.hoz.netapi.client.config.DataConfig
import network.hoz.kaffeine.get
import network.hoz.kaffeine.set
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.util.*
import javax.inject.Inject

class NetGameProvider(
    private val gameService: NetGameServiceClient,
    private val clientConfig: DataConfig,
    private val gameTypeMessage: MGameType
) : Controlled {
    private val logger = KotlinLogging.logger {}

    /**
     * Stores name-to-uuid values for games.
     */
    private val gameNameToUUID: MutableMap<String, UUID> = mutableMapOf()

    private val gameCache: Cache<UUID, ProtoGameFrame> = Caffeine.newBuilder().build()

    private val configCache: Cache<String, GameConfig> = Caffeine.newBuilder().build()

    private val storeCache: Cache<String, StoreHolder> = Caffeine.newBuilder().build()

    private val spawnerCache: Cache<String, ProtoSpawnerType> = Caffeine.newBuilder().build()

    @Inject
    @Suppress("unused") // constructor needed for guice, don't remove
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
    fun oneGame(gameId: UUID): DataResultable<ProtoGameFrame> = DataResultable.failIfNull(gameCache[gameId], "Game not found.")

    /**
     * Tries to get one game with given name from cache.
     *
     * @param name name of the game
     * @return [DataResultable] with result
     */
    fun oneGame(name: String): DataResultable<ProtoGameFrame> = gameNameToUUID[name]?.let { oneGame(it) } ?: DataResultable.fail("Game not found.")

    /**
     * Gets all available games from cache.
     *
     * @return Collection of games.
     */
    fun allGames(): Collection<ProtoGameFrame> = gameCache.asMap().values // TODO: add extension for Cache#values

    /**
     * Tries to get one config with given name from cache.
     *
     * @param name name of the config
     * @return [DataResultable] with result
     */
    fun oneConfig(name: String): DataResultable<GameConfig> = DataResultable.failIfNull(configCache[name], "Config not found.")

    /**
     * Gets all available configs from cache.
     *
     * @return Collection of configs.
     */
    fun allConfigs(): Collection<GameConfig> = configCache.asMap().values // TODO: add extension for Cache#values

    /**
     * Tries to get one store with given name from cache.
     *
     * @param name name of the store
     * @return [DataResultable] with result
     */
    fun oneStore(name: String): DataResultable<StoreHolder> = DataResultable.failIfNull(storeCache[name], "Store not found.")

    /**
     * Gets all available stores from cache.
     *
     * @return Collection of stores.
     */
    fun allStores(): Collection<StoreHolder> = storeCache.asMap().values // TODO: add extension for Cache#values

    /**
     * Tries to get one spawner with given name from cache.
     *
     * @param name name of the spawner
     * @return [DataResultable] with result
     */
    fun oneSpawner(name: String): DataResultable<ProtoSpawnerType> = DataResultable.failIfNull(spawnerCache[name], "Spawner not found.")

    /**
     * Gets all available spawners from cache.
     *
     * @return Collection of spawners.
     */
    fun allSpawners(): Collection<ProtoSpawnerType> = spawnerCache.asMap().values // TODO: add extension for Cache#values

    /**
     * Tries to retrieve the game from BAGR backend.
     *
     * @param gameId game id
     * @return [DataResultable] of the operation.
     */
    fun loadGame(gameId: UUID): Mono<DataResultable<ProtoGameFrame>> = doGameLoading(
        gameService.oneById(
            WUUID.newBuilder()
                .setValue(gameId.toString())
                .build()
        )
    )

    /**
     * Tries to retrieve the game from backend.
     *
     * @param name name of the game
     * @return [DataResultable] of the operation.
     */
    fun loadGame(name: String): Mono<DataResultable<ProtoGameFrame>> = doGameLoading(
        gameService.oneByName(
            StringValue.newBuilder()
                .setValue(name)
                .build()
        )
    )

    /**
     * Tries to save given game to the backend and replaces it in the cache.
     *
     * @param frame game to save.
     * @return [DataResultable] result of this operation.
     */
    // TODO: check this
    fun saveGame(frame: ProtoGameFrame): Mono<DataResultable<UUID>> = gameService.saveGame(frame)
        .map { DataResultable.from(it.result, UUID.fromString(frame.uuid)) }
        .ifOk { gameCache[it] = frame }
        .onErrorHandle(logger)

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
                spawnerCache[spawnerName] = spawnerTypeHolder
                logger.debug { "Saved new spawner [$spawnerName] for GameType[$gameTypeMessage]." }
            }
            .onErrorHandle(logger)
    }

    /**
     * Retrieves all available games from backend.
     *
     * @return Flux of [ProtoGameFrame]
     */
    fun loadGames(): Flux<ProtoGameFrame> = gameService.all(gameTypeMessage)
        .doOnNext { logger.debug { "Received game [${it.name}] - [${it.uuid}] for GameType[${it.type}]." } }
        .onErrorHandle(logger)

    /**
     * Retrieves all available configs from backend.
     *
     * @return Flux of [GameConfig]
     */
    fun loadConfigs(): Flux<GameConfig> = gameService.allConfigs(gameTypeMessage)
        .doOnNext { logger.debug { "Received config [${it.name}] for GameType[${it.type}]." } }
        .onErrorHandle(logger)

    /**
     * Retrieves all available stores from backend.
     *
     * @return Flux of [StoreHolder]
     */
    fun loadStores(): Flux<StoreHolder> = gameService.allStores(gameTypeMessage)
        .doOnNext { logger.debug { "Received store holder [${it.name}] - [${it.uuid}] for GameType[${it.gameType}]." } }
        .onErrorHandle(logger)

    /**
     * Retrieves all available spawners from backend.
     *
     * @return Flux of [ProtoSpawnerType]
     */
    fun loadSpawners(): Flux<ProtoSpawnerType> = gameService.allSpawnerTypes(gameTypeMessage)
        .doOnNext { logger.debug { "Received spawner [${it.name}] for GameType[${it.type}]." } }
        .onErrorHandle(logger)

    /**
     * Processes the given [ResultableData] into a [ProtoGameFrame].
     *
     * @param loadingMono mono from the backend
     * @return mono with [DataResultable] result of the operation.
     */
    private fun doGameLoading(loadingMono: Mono<ResultableData>): Mono<DataResultable<ProtoGameFrame>> = loadingMono.unpack(ProtoGameFrame::class)
        .ifOk {
            val uuid = UUID.fromString(it.uuid)

            logger.debug { "Received game [${it.name}] - [$uuid] for GameType[${it.type}]" }
            gameCache[uuid] = it
        }
        .onErrorHandle(logger)

    /**
     * Loads all cacheable values from the backend.
     * The order shouldn't be important at all.
     */
    private fun createDataCache() {
        loadGames()
            .doOnNext {
                val gameId = UUID.fromString(it.uuid)

                logger.trace { "Caching new game [${it.name}]..." }
                gameCache[gameId] = it
                gameNameToUUID[it.name] = gameId
            }
            .onErrorHandle(logger)
            .subscribe()

        loadConfigs()
            .doOnNext {
                logger.trace { "Caching new config [${it.name}]..." }
                configCache[it.name] = it
            }
            .onErrorHandle(logger)
            .subscribe()

        loadStores()
            .doOnNext {
                logger.trace { "Caching new store [${it.name}]..." }
                storeCache[it.name] = it
            }
            .onErrorHandle(logger)
            .subscribe()

        loadSpawners()
            .doOnNext {
                logger.trace { "Caching new spawner type [${it.name}]..." }
                spawnerCache[it.name] = it
            }
            .onErrorHandle(logger)
            .subscribe()
    }

    private fun subscribeForUpdates() {}
}