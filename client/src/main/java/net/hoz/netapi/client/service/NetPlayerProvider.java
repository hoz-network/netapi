package net.hoz.netapi.client.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.iamceph.resulter.core.DataResultable;
import com.iamceph.resulter.core.Resultable;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.data.*;
import net.hoz.api.service.NetPlayerServiceClient;
import net.hoz.api.util.ReactorHelper;
import net.hoz.netapi.client.util.NetUtils;
import net.hoz.netapi.client.util.Unpacker;
import org.screamingsandals.lib.utils.Controllable;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/**
 * Service for managing generally players.
 */
@Slf4j
public class NetPlayerProvider implements Disposable {
    /**
     * Cache of the player data.
     */
    private final Cache<UUID, NetPlayer> playerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();
    /**
     * Cache of the player history.
     */
    private final Cache<UUID, NetPlayerHistory> historyCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    /**
     * RSocket service.
     */
    private final NetPlayerServiceClient netPlayerService;

    @Accessors(fluent = true)
    @Getter
    private final Settings settings;

    private Disposable updateListener;

    @Inject
    public NetPlayerProvider(NetPlayerServiceClient netPlayerService,
                             Controllable controllable) {
        this.netPlayerService = netPlayerService;
        this.settings = new Settings(this);

        controllable.enable(this::subscribeToUpdates);
        controllable.preDisable(this::dispose);
    }

    @Override
    public void dispose() {
        if (updateListener != null) {
            updateListener.dispose();
        }

        playerCache.invalidateAll();
        historyCache.invalidateAll();
    }

    /**
     * Tries to get the {@link NetPlayer} from the cache.
     *
     * @param uuid ID of the player
     * @return {@link DataResultable} result of the operation.
     */
    public DataResultable<NetPlayer> getPlayer(UUID uuid) {
        return DataResultable.failIfNull(playerCache.getIfPresent(uuid));
    }

    /**
     * Tries to get the {@link NetPlayerHistory} from the cache.
     *
     * @param uuid ID of the player
     * @return {@link DataResultable} result of the operation.
     */
    public DataResultable<NetPlayerHistory> getHistory(UUID uuid) {
        return DataResultable.failIfNull(historyCache.getIfPresent(uuid));
    }

    /**
     * Tries to get the {@link NetPlayer} data container from the BAGR instance.
     *
     * @param uuid
     * @return
     */
    public Mono<NetPlayer> getPlayerNow(UUID uuid) {
        return netPlayerService.dataFor(WUUID.newBuilder()
                        .setValue(uuid.toString())
                        .build())
                .filter(ReactorHelper.filterResult(log))
                .map(response -> {
                    final var netPlayer = Unpacker.unpackUnsafe(response.getData(), NetPlayer.class);
                    playerCache.put(uuid, netPlayer);

                    return netPlayer;
                })
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    /**
     * Tries to get the {@link NetPlayerHistory} data container from the BAGR instance.
     *
     * @param uuid
     * @return
     */
    public Mono<NetPlayerHistory> getHistoryNow(UUID uuid) {
        return netPlayerService.historyFor(WUUID.newBuilder()
                        .setValue(uuid.toString())
                        .build())
                .filter(ReactorHelper.filterResult(log))
                .map(response -> {
                    final var netPlayerHistory = Unpacker.unpackUnsafe(response.getData(), NetPlayerHistory.class);
                    historyCache.put(uuid, netPlayerHistory);

                    return netPlayerHistory;
                })
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<Resultable> updateData(NetPlayer netPlayer) {
        return netPlayerService.updateData(netPlayer)
                .map(Resultable::convert)
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<Resultable> playerOnline(UUID uuid) {
        return netPlayerService.playerOnline(WUUID.newBuilder()
                        .setValue(uuid.toString())
                        .build())
                .map(Resultable::convert)
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<Resultable> playerOffline(UUID uuid) {
        return netPlayerService.playerOffline(WUUID.newBuilder()
                        .setValue(uuid.toString())
                        .build())
                .map(Resultable::convert)
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public DataResultable<Locale> resolveLocale(UUID uuid) {
        final var data = getPlayer(uuid);
        if (data.isFail()) {
            return data.transform();
        }
        return NetUtils.resolveLocale(data.data().getSettings().getLocale());
    }

    private void subscribeToUpdates() {
        if (updateListener != null) {
            updateListener.dispose();
        }

        updateListener = netPlayerService.subscribeToUpdates(Empty.getDefaultInstance())
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe(data -> {
                    final var uuid = UUID.fromString(data.getOwner().getUuid());
                    playerCache.invalidate(uuid);
                    playerCache.put(uuid, data);
                });
    }

    public record Settings(NetPlayerProvider client) {
        public Mono<Boolean> forKey(UUID uuid, PlayerSettings.Key key) {
            return Mono.fromSupplier(() -> client.playerCache.getIfPresent(uuid))
                    .map(next -> next.getSettings()
                            .getSettingsMap()
                            .get(key.getNumber()))
                    .switchIfEmpty(forKeyUncached(uuid, key));
        }

        private Mono<Boolean> forKeyUncached(UUID uuid, PlayerSettings.Key key) {
            return client.getPlayerNow(uuid)
                    .map(netPlayer -> netPlayer
                            .getSettings()
                            .getSettingsMap()
                            .get(key.getNumber()))
                    .defaultIfEmpty(false);
        }
    }
}
