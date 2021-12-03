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
import net.hoz.netapi.client.util.Unpacker;
import org.screamingsandals.lib.utils.Controllable;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Slf4j
public class PlayerDataProvider {
    private final Cache<UUID, NetPlayer> playerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();
    private final Cache<UUID, NetPlayerHistory> historyCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    private final NetPlayerServiceClient netPlayerService;

    @Accessors(fluent = true)
    @Getter
    private final Settings settings;

    private Disposable updateListener;

    @Inject
    public PlayerDataProvider(NetPlayerServiceClient netPlayerService,
                              Controllable controllable) {
        this.netPlayerService = netPlayerService;
        this.settings = new Settings(this);

        listenForUpdates();

        controllable.preDisable(() -> {
            if (updateListener != null) {
                updateListener.dispose();
            }
        });
    }

    public DataResultable<NetPlayer> getDataCached(UUID uuid) {
        return DataResultable.failIfNull(playerCache.getIfPresent(uuid));
    }

    public Mono<DataResultable<NetPlayer>> getData(UUID uuid) {
        final var data = playerCache.getIfPresent(uuid);
        if (data != null) {
            return Mono.just(DataResultable.ok(data));
        }

        return netPlayerService.dataFor(WUUID.newBuilder()
                        .setValue(uuid.toString())
                        .build())
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log))
                .filter(ReactorHelper.filterResult(log))
                .map(response -> {
                    final var netPlayer = Unpacker.unpackUnsafe(response.getData(), NetPlayer.class);
                    playerCache.put(uuid, netPlayer);

                    return DataResultable.ok(netPlayer);
                });
    }

    public Mono<DataResultable<NetPlayerHistory>> getHistory(UUID uuid) {
        return netPlayerService.historyFor(WUUID.newBuilder()
                        .setValue(uuid.toString())
                        .build())
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log))
                .filter(ReactorHelper.filterResult(log))
                .map(response -> {
                    final var netPlayerHistory = Unpacker.unpackUnsafe(response.getData(), NetPlayerHistory.class);
                    historyCache.put(uuid, netPlayerHistory);

                    return DataResultable.ok(netPlayerHistory);
                });
    }

    public Mono<Resultable> updateData(UUID uuid, NetPlayer netPlayer) {
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

    private void listenForUpdates() {
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

    public record Settings(PlayerDataProvider client) {
        public Mono<Boolean> forKey(UUID uuid, PlayerSettings.Key key) {
            return Mono.fromSupplier(() -> client.playerCache.getIfPresent(uuid))
                    .map(next -> next.getSettings()
                            .getSettingsMap()
                            .get(key.getNumber()))
                    .switchIfEmpty(getNonCached(uuid, key));
        }

        private Mono<Boolean> getNonCached(UUID uuid, PlayerSettings.Key key) {
            return client.getData(uuid)
                    .map(result -> {
                        if (result.isOk()) {
                            return result.data()
                                    .getSettings()
                                    .getSettingsMap()
                                    .get(key.getNumber());
                        }

                        log.warn("Getting settings[{}] for player[{}] failed: {}", key, uuid, result.message());
                        return false;
                    });
        }
    }
}
