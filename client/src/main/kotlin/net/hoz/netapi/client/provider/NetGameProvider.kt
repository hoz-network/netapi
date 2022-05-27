/*
 * Copyright 2022 hoz-network.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.hoz.netapi.client.provider

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.stringValue
import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.core.model.ResultableData
import com.iamceph.resulter.kotlin.resultable
import com.iamceph.resulter.kotlin.unpack
import kotlinx.coroutines.flow.*
import mu.KotlinLogging
import net.hoz.api.data.WUUID
import net.hoz.api.data.game.GameConfig
import net.hoz.api.data.game.ProtoGameFrame
import net.hoz.api.data.game.ProtoSpawnerType
import net.hoz.api.data.game.StoreHolder
import net.hoz.api.data.wUUID
import net.hoz.api.service.MGameType
import net.hoz.api.service.NetGameServiceGrpcKt
import net.hoz.api.service.mGameType
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.onErrorHandle
import net.hoz.netapi.client.config.DataConfig
import net.hoz.netproto.asUuid
import network.hoz.kaffeine.get
import network.hoz.kaffeine.set
import java.util.*
import javax.inject.Inject

private val log = KotlinLogging.logger {}

class NetGameProvider @Inject constructor(
    private val gameService: NetGameServiceGrpcKt.NetGameServiceCoroutineStub,
    private val clientConfig: DataConfig,
    private val gameType: MGameType = mGameType { type = clientConfig.gameType }
) : Controlled {

    /**
     * Stores name-to-uuid values for games.
     */
    private val gameNameToUUID: MutableMap<String, UUID> = mutableMapOf()

    private val gameCache: Cache<UUID, ProtoGameFrame> = Caffeine.newBuilder().build()

    private val configCache: Cache<String, GameConfig> = Caffeine.newBuilder().build()

    private val storeCache: Cache<String, StoreHolder> = Caffeine.newBuilder().build()

    private val spawnerCache: Cache<String, ProtoSpawnerType> = Caffeine.newBuilder().build()

    override suspend fun initialize() {
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
    fun oneGame(gameId: UUID): DataResultable<ProtoGameFrame> =
        DataResultable.failIfNull(gameCache[gameId], "Game not found.")

    /**
     * Tries to get one game with given name from cache.
     *
     * @param name name of the game
     * @return [DataResultable] with result
     */
    fun oneGame(name: String): DataResultable<ProtoGameFrame> =
        gameNameToUUID[name]?.let { oneGame(it) } ?: DataResultable.fail("Game not found.")

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
    fun oneConfig(name: String): DataResultable<GameConfig> =
        DataResultable.failIfNull(configCache[name], "Config not found.")

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
    fun oneStore(name: String): DataResultable<StoreHolder> =
        DataResultable.failIfNull(storeCache[name], "Store not found.")

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
    fun oneSpawner(name: String): DataResultable<ProtoSpawnerType> =
        DataResultable.failIfNull(spawnerCache[name], "Spawner not found.")

    /**
     * Gets all available spawners from cache.
     *
     * @return Collection of spawners.
     */
    fun allSpawners(): Collection<ProtoSpawnerType> =
        spawnerCache.asMap().values // TODO: add extension for Cache#values

    /**
     * Tries to retrieve the game from BAGR backend.
     *
     * @param gameId game id
     * @return [DataResultable] of the operation.
     */
    suspend fun loadGame(gameId: UUID): DataResultable<ProtoGameFrame> =
        handleGameData(
            gameService.oneById(
                wUUID { gameId.toString() }
            )
        )

    /**
     * Tries to retrieve the game from backend.
     *
     * @param name name of the game
     * @return [DataResultable] of the operation.
     */
    suspend fun loadGame(name: String): DataResultable<ProtoGameFrame> =
        handleGameData(
            gameService.oneByName(
                stringValue { value = name }
            )
        )

    /**
     * Tries to save given game to the backend and replaces it in the cache.
     *
     * @param frame game to save.
     * @return [DataResultable] result of this operation.
     */
    // TODO: check this
    suspend fun saveGame(frame: ProtoGameFrame): DataResultable<UUID> {
        return gameService.saveGame(frame)
            .unpack(WUUID::class)
            .map { it.asUuid() }
            .ifOk { gameCache[it] = frame }
    }

    /**
     * Tries to save given spawner to the backend and caches it.
     *
     * @param spawnerTypeHolder holder to save
     * @return [Resultable] result of this operation.
     */
    suspend fun saveSpawnerType(spawnerTypeHolder: ProtoSpawnerType): Resultable {
        return gameService.saveSpawnerType(spawnerTypeHolder)
            .resultable()
            .also {
                if (it.isOk) {
                    val spawnerName = spawnerTypeHolder.name

                    spawnerCache[spawnerName] = spawnerTypeHolder
                    log.debug { "Saved new spawner [$spawnerName] for GameType[$gameType]." }
                }
            }
    }

    /**
     * Retrieves all available games from backend.
     *
     * @return Flux of [ProtoGameFrame]
     */
    fun loadGames(): Flow<ProtoGameFrame> = gameService.all(gameType)
        .onEach { log.debug { "Received game [${it.name}] - [${it.uuid}] for GameType[${it.type}]." } }
        .onErrorHandle(log)


    /**
     * Retrieves all available configs from backend.
     *
     * @return Flux of [GameConfig]
     */
    fun loadConfigs(): Flow<GameConfig> = gameService.allConfigs(gameType)
        .onEach { log.debug { "Received config [${it.name}] for GameType[${it.type}]." } }
        .onErrorHandle(log)

    /**
     * Retrieves all available stores from backend.
     *
     * @return Flux of [StoreHolder]
     */
    fun loadStores(): Flow<StoreHolder> = gameService.allStores(gameType)
        .onEach { log.debug { "Received store holder [${it.name}] - [${it.id}] for GameType[${it.gameType}]." } }
        .onErrorHandle(log)

    /**
     * Retrieves all available spawners from backend.
     *
     * @return Flux of [ProtoSpawnerType]
     */
    fun loadSpawners(): Flow<ProtoSpawnerType> = gameService.allSpawnerTypes(gameType)
        .onEach { log.debug { "Received spawner [${it.name}] for GameType[${it.type}]." } }
        .onErrorHandle(log)

    /**
     * Processes the given [ResultableData] into a [ProtoGameFrame].
     *
     * @param loadingMono mono from the backend
     * @return mono with [DataResultable] result of the operation.
     */
    private fun handleGameData(data: ResultableData): DataResultable<ProtoGameFrame> =
        data.unpack(ProtoGameFrame::class)
            .ifOk {
                val uuid = UUID.fromString(it.uuid)

                log.debug { "Received game [${it.name}] - [$uuid] for GameType[${it.type}]" }
                gameCache[uuid] = it
            }

    /**
     * Loads all cacheable values from the backend.
     * The order shouldn't be important at all.
     */
    private suspend fun createDataCache() {
        loadGames()
            .onEach {
                val gameId = UUID.fromString(it.uuid)

                log.trace { "Caching new game [${it.name}]..." }
                gameCache[gameId] = it
                gameNameToUUID[it.name] = gameId
            }
            .collect()

        loadConfigs()
            .onEach {
                log.trace { "Caching new config [${it.name}]..." }
                configCache[it.name] = it
            }
            .onErrorHandle(log)
            .collect()

        loadStores()
            .onEach {
                log.trace { "Caching new store [${it.name}]..." }
                storeCache[it.name] = it
            }
            .onErrorHandle(log)
            .collect()

        loadSpawners()
            .onEach {
                log.trace { "Caching new spawner type [${it.name}]..." }
                spawnerCache[it.name] = it
            }
            .onErrorHandle(log)
            .collect()
    }

    private fun subscribeForUpdates() {
        //TODO: handle game updates
    }
}