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

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.Empty
import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.kotlin.dataResultable
import com.iamceph.resulter.kotlin.resultable
import com.iamceph.resulter.kotlin.unpack
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.hoz.api.data.NetPlayer
import net.hoz.api.data.PlayerHistory
import net.hoz.api.service.NetPlayerServiceGrpcKt
import net.hoz.api.service.SettingsKeyUpdate
import net.hoz.api.service.netPlayerRequest
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.onErrorHandle
import net.hoz.netapi.client.util.NetUtils
import net.hoz.netproto.asProto
import org.screamingsandals.lib.player.PlayerMapper.getPlayer
import org.slf4j.Logger
import java.time.Duration
import java.util.*
import javax.inject.Inject

private val log: Logger = KotlinLogging.logger {}

class NetPlayerProvider @Inject constructor(
    private val playerService: NetPlayerServiceGrpcKt.NetPlayerServiceCoroutineStub,
    private val playerUpdater: MutableSharedFlow<NetPlayer>
) : Controlled {
    /**
     * Cache of the player data.
     */
    private val playerCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(30))
        .build<UUID, NetPlayer>()

    /**
     * Cache of the player history.
     */
    private val historyCache = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(30))
        .build<UUID, PlayerHistory>()

    private var playerUpdateJob: Job? = null

    override suspend fun initialize() {
        playerUpdateJob?.cancel("Staring new listening for NetPlayer updates")

        playerUpdateJob = coroutineScope {
            launch {
                playerService.subscribeToUpdates(Empty.getDefaultInstance())
                    .onEach { data: NetPlayer ->
                        val uuid = UUID.fromString(data.id)
                        playerCache.put(uuid, data)
                        playerUpdater.tryEmit(data)
                    }
                    .onErrorHandle(log)
                    .collect()
            }
        }
    }

    override fun dispose() {
        playerUpdateJob?.cancel("Ending listening for Player updates.")

        playerCache.invalidateAll()
        historyCache.invalidateAll()
    }

    fun playerUpdater(): SharedFlow<NetPlayer> = playerUpdater

    /**
     * Provides [NetPlayer] for given [UUID]
     */
    fun player(uuid: UUID): NetPlayer? = playerCache.getIfPresent(uuid)

    /**
     * Provides [PlayerHistory] for given [UUID]
     */
    fun history(uuid: UUID): PlayerHistory? = historyCache.getIfPresent(uuid)

    /**
     * Tries to get the [NetPlayer] data container from the BAGR instance.
     */
    suspend fun loadPlayer(uuid: UUID, address: String?): DataResultable<NetPlayer> {
        return playerService.dataFor(
            netPlayerRequest {
                this.id = uuid.toString()
                this.address = address
                    ?: ""
            }
        )
            .unpack(NetPlayer::class)
            .ifOk { playerCache.put(uuid, it) }
    }

    /**
     * Tries to get the [PlayerHistory] data container from the BAGR instance.
     */
    suspend fun loadPlayerHistory(uuid: UUID): DataResultable<PlayerHistory> {
        return playerService.historyFor(uuid.asProto())
            .unpack(PlayerHistory::class)
            .ifOk { historyCache.put(uuid, it) }
    }

    suspend fun updateSettings(settings: SettingsKeyUpdate): Resultable = playerService.updateSettingsByKey(settings)
        .resultable()

    suspend fun playerOnline(uuid: UUID): Resultable = playerService.playerOnline(uuid.asProto())
        .resultable()

    suspend fun playerOffline(uuid: UUID): Resultable = playerService.playerOffline(uuid.asProto())
        .resultable()

    fun resolveLocale(uuid: UUID): DataResultable<Locale> {
        val player = player(uuid)
            ?: return DataResultable.fail("Player not found.")
        return NetUtils.resolveLocale(player.settings.locale)
    }
}