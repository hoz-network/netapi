package net.hoz.netapi.client.provider

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.Empty
import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.kotlin.ifOk
import com.iamceph.resulter.kotlin.resultable
import com.iamceph.resulter.kotlin.unpack
import net.hoz.api.data.NetPlayer
import net.hoz.api.data.NetPlayerHistory
import net.hoz.api.data.WUUID
import net.hoz.api.service.NetPlayerRequest
import net.hoz.api.service.NetPlayerServiceClient
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.onErrorHandle
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.*
import javax.inject.Inject

class NetPlayerProvider @Inject constructor(private val netPlayerService: NetPlayerServiceClient) : Controlled {
    private val log = LoggerFactory.getLogger(javaClass)
    private val playerUpdater: Sinks.Many<NetPlayer> = Sinks.many().multicast().directBestEffort()

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
        .build<UUID, NetPlayerHistory>()

    private var updateListener: Disposable? = null

    override fun dispose() {
        updateListener?.dispose()

        playerCache.invalidateAll()
        historyCache.invalidateAll()
    }

    override fun initialize() {
        updateListener?.dispose()

        updateListener = netPlayerService.subscribeToUpdates(Empty.getDefaultInstance())
            .doOnNext { data: NetPlayer ->
                val uuid = UUID.fromString(data.owner.uuid)
                playerCache.put(uuid, data)
                playerUpdater.tryEmitNext(data)
            }
            .onErrorHandle(log)
            .subscribe()
    }

    /**
     * Tries to get the [NetPlayer] from the cache.
     *
     * @param uuid ID of the player
     * @return [DataResultable] result of the operation.
     */
    fun getPlayer(uuid: UUID): DataResultable<NetPlayer> = DataResultable.failIfNull(playerCache.getIfPresent(uuid))

    /**
     * Tries to get the [NetPlayerHistory] from the cache.
     *
     * @param uuid ID of the player
     * @return [DataResultable] result of the operation.
     */
    fun getHistory(uuid: UUID): DataResultable<NetPlayerHistory> = DataResultable.failIfNull(historyCache.getIfPresent(uuid))

    /**
     * Tries to get the [NetPlayer] data container from the BAGR instance.
     *
     * @param uuid
     * @param address
     * @return
     */
    fun loadPlayer(uuid: UUID, address: String): Mono<DataResultable<NetPlayer>> {
        return netPlayerService.dataFor(
            NetPlayerRequest.newBuilder()
                .setUUID(uuid.toString())
                .setAddress(address)
                .build()
        )
        .unpack(NetPlayer::class)
        .ifOk { playerCache.put(uuid, it) }
        .onErrorHandle(log)
    }

    /**
     * Tries to get the [NetPlayerHistory] data container from the BAGR instance.
     *
     * @param uuid
     * @return
     */
    fun loadPlayerHistory(uuid: UUID): Mono<DataResultable<NetPlayerHistory>> {
        return netPlayerService.historyFor(
            WUUID.newBuilder()
                .setValue(uuid.toString())
                .build()
        )
        .unpack(NetPlayerHistory::class)
        .ifOk { historyCache.put(uuid, it) }
        .onErrorHandle(log)
    }

    fun updateData(netPlayer: NetPlayer?): Mono<Resultable> = netPlayerService.updateData(netPlayer)
        .resultable()
        .onErrorHandle(log)

    fun playerOnline(uuid: UUID): Mono<Resultable> {
        return netPlayerService.playerOnline(
            WUUID.newBuilder()
                .setValue(uuid.toString())
                .build()
        )
        .resultable()
        .onErrorHandle(log)
    }

    fun playerOffline(uuid: UUID): Mono<Resultable> {
        return netPlayerService.playerOffline(
            WUUID.newBuilder()
                .setValue(uuid.toString())
                .build()
        )
        .resultable()
        .onErrorHandle(log)
    }

    fun resolveLocale(uuid: UUID): DataResultable<Locale> {
        val data = getPlayer(uuid)
        return if (data.isFail) {
            data.transform()
        } else net.hoz.netapi.client.util.resolveLocale(data.data().settings.locale)
    }

    fun playerUpdater(): Flux<NetPlayer> = playerUpdater.asFlux()
}