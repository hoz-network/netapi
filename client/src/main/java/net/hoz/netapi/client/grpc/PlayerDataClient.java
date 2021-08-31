package net.hoz.netapi.client.grpc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.iamceph.resulter.core.DataResult;
import com.iamceph.resulter.core.SimpleResult;
import com.iamceph.resulter.core.api.DataResultable;
import com.iamceph.resulter.core.api.Resultable;
import com.iamceph.resulter.core.model.Result;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.data.player.PlayerDataContainer;
import net.hoz.api.data.player.PlayerSettings;
import net.hoz.api.data.player.PlayerState;
import net.hoz.api.service.player.*;
import net.hoz.netapi.grpc.service.GrpcStubService;
import net.hoz.netapi.grpc.util.ReactorHelper;
import org.apache.commons.lang.LocaleUtils;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

//TODO: rework this
@Slf4j
public class PlayerDataClient {
    private final Cache<UUID, PlayerDataContainer> containerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    private final AtomicReference<ReactorPlayerDataServiceGrpc.ReactorPlayerDataServiceStub> stub;

    @Accessors(fluent = true)
    @Getter
    private final State state;
    @Accessors(fluent = true)
    @Getter
    private final Settings settings;

    private Disposable listener;

    @Inject
    public PlayerDataClient(GrpcStubService stubService) {
        final var holder = stubService.getHolder(ReactorPlayerDataServiceGrpc.class);
        this.stub = holder.getStub(ReactorPlayerDataServiceGrpc.ReactorPlayerDataServiceStub.class);

        this.state = new State(this);
        this.settings = new Settings(this);

        holder.getChannel().renewCallback(this::listenForUpdates);
        listenForUpdates();
    }

    public DataResultable<PlayerDataContainer> getPlayerDataCached(UUID uuid) {
        return DataResult.failIfNull(containerCache.getIfPresent(uuid));
    }

    public Mono<DataResultable<PlayerDataContainer>> getPlayerData(UUID uuid) {
        return stub.get()
                .dataFor(PlayerIdHolder.newBuilder()
                        .setUuid(uuid.toString())
                        .build())
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log))
                .map(response -> {
                    final var result = SimpleResult.convert(response.getResult());
                    if (result.isOk()) {
                        containerCache.invalidate(uuid);
                        containerCache.put(uuid, response.getPlayerData());
                        return DataResult.ok(response.getPlayerData());
                    }
                    return result.transform();
                });
    }

    public Mono<Resultable> updatePlayerData(UUID uuid, PlayerDataContainer container) {
        return stub.get()
                .dataUpdateFor(PlayerDataUpdate.newBuilder()
                        .setPlayerData(container)
                        .setUuid(uuid.toString())
                        .build())
                .map(next -> SimpleResult.ok())
                .onErrorReturn(SimpleResult.fail("GRPC fail.")); //TODO
    }

    public Mono<PlayerStatusHistoryResult> getPlayerHistory(UUID uuid) {
        return stub.get()
                .historyFor(PlayerIdHolder.newBuilder()
                        .setUuid(uuid.toString())
                        .build())
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<Locale> getPlayerLocale(UUID uuid, String address) {
        return stub.get()
                .languageFor(PlayerLanguageRequest.newBuilder()
                        .setUuid(uuid.toString())
                        .setAddress(address)
                        .build())
                .map(next -> LocaleUtils.toLocale(next.getLocale()))
                .onErrorReturn(Locale.ENGLISH)
                .defaultIfEmpty(Locale.ENGLISH);
    }

    private void listenForUpdates() {
        if (listener != null) {
            listener.dispose();
        }

        listener = stub.get()
                .subscribe(Empty.getDefaultInstance())
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe(data -> {
                    final var uuid = UUID.fromString(data.getOwnableData().getOwner());
                    containerCache.invalidate(uuid);
                    containerCache.put(uuid, data);
                });
    }

    public record State(PlayerDataClient client) {
        public Mono<Resultable> online(UUID uuid) {
            return changeInternal(uuid, PlayerState.ONLINE)
                    .map(next -> SimpleResult.convert(next.getResult()))
                    .defaultIfEmpty(SimpleResult.fail("Operation failed due to internal error."));
        }

        public Mono<Resultable> offline(UUID uuid) {
            return changeInternal(uuid, PlayerState.OFFLINE)
                    .map(next -> SimpleResult.convert(next.getResult()))
                    .defaultIfEmpty(SimpleResult.fail("Operation failed due to internal error."));
        }

        private Mono<PlayerStatusChangeResult> changeInternal(UUID uuid, PlayerState newState) {
            return client.stub
                    .get()
                    .changeStatusFor(PlayerStatusChangeRequest.newBuilder()
                            .setUuid(uuid.toString())
                            .setNewState(newState)
                            .build())
                    .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
        }
    }

    public record Settings(PlayerDataClient client) {
        public Mono<Boolean> forKey(UUID uuid, PlayerSettings.Key key) {
            return Mono.fromSupplier(() -> client.containerCache.getIfPresent(uuid))
                    .map(next -> next.getSettings()
                            .getSettingsMap()
                            .get(key.getNumber()))
                    .switchIfEmpty(getNonCached(uuid, key));
        }

        private Mono<Boolean> getNonCached(UUID uuid, PlayerSettings.Key key) {
            return client.getPlayerData(uuid)
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
