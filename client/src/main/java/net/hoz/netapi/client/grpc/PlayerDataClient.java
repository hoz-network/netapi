package net.hoz.netapi.client.grpc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.Empty;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.commons.Result;
import net.hoz.api.data.player.PlayerDataContainer;
import net.hoz.api.data.player.PlayerSettings;
import net.hoz.api.data.player.PlayerState;
import net.hoz.api.result.DataResult;
import net.hoz.api.result.SimpleResult;
import net.hoz.api.service.player.*;
import net.hoz.netapi.grpc.service.GrpcStubService;
import net.hoz.netapi.grpc.util.ReactorHelper;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class PlayerDataClient {
    private final Cache<UUID, PlayerDataContainer> containerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();
    private final AtomicReference<ReactorPlayerDataServiceGrpc.ReactorPlayerDataServiceStub> stub;
    private Disposable listener;

    @Inject
    public PlayerDataClient(GrpcStubService stubService) {
        final var holder = stubService.getHolder(ReactorPlayerDataServiceGrpc.class);
        this.stub = holder.getStub(ReactorPlayerDataServiceGrpc.ReactorPlayerDataServiceStub.class);

        holder.getChannel().renewCallback(this::listenForUpdates);
        listenForUpdates();
    }

    public DataResult<PlayerDataContainer> getPlayerDataCached(UUID uuid) {
        return DataResult.failIfNull(containerCache.getIfPresent(uuid));
    }

    public Mono<DataResult<PlayerDataContainer>> getPlayerData(UUID uuid) {
        return stub.get()
                .requestPlayerData(PlayerDataRequest.newBuilder()
                        .setUuid(uuid.toString())
                        .build())
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log))
                .map(response -> {
                    final var result = response.getResult();
                    if (result.getStatus() == Result.Status.OK) {
                        containerCache.invalidate(uuid);
                        containerCache.put(uuid, response.getPlayerData());
                        return DataResult.okData(response.getPlayerData());
                    }

                    return DataResult.convert(result);
                });
    }

    public Mono<SimpleResult> updatePlayerData(UUID uuid, PlayerDataContainer container) {
        return stub.get()
                .updatePlayerData(PlayerDataUpdate.newBuilder()
                        .setPlayerData(container)
                        .setUuid(uuid.toString())
                        .build())
                .map(next -> SimpleResult.ok())
                .onErrorReturn(SimpleResult.fail("GRPC fail.")); //TODO
    }

    public Mono<PlayerStatusHistoryResult> getPlayerHistory(UUID uuid) {
        return stub.get()
                .requestPlayerHistory(PlayerStatusHistoryRequest.newBuilder()
                        .setUuid(uuid.toString())
                        .build())
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<PlayerStatusChangeResult> changePlayerState(UUID uuid, PlayerState newState) {
        return stub.get()
                .requestStatusChange(PlayerStatusChangeRequest.newBuilder()
                        .setUuid(uuid.toString())
                        .setNewState(newState)
                        .build())
                .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
    }

    public Mono<DataResult<Boolean>> getSettingsForKey(UUID uuid, PlayerSettings.Key key) {
        final var cachedData = getPlayerDataCached(uuid);
        if (cachedData.isOk()) {
            return Mono.just(DataResult.okData(
                    cachedData.getData()
                            .getSettings()
                            .getSettingsMap()
                            .get(key.getNumber())))
                    .onErrorResume(ex -> ReactorHelper.monoError(ex, log));
        }

        return getPlayerData(uuid)
                .map(result -> {
                    if (result.isOk() && result.hasData()) {
                        return DataResult.okData(
                                result.getData()
                                        .getSettings()
                                        .getSettingsMap()
                                        .get(key.getNumber()));
                    }
                    return DataResult.fail(result.getMessage());
                });
    }

    private void listenForUpdates() {
        if (listener != null) {
            listener.dispose();
        }

        listener = stub.get()
                .listenForPlayerDataUpdates(Empty.getDefaultInstance())
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe(data -> {
                    final var uuid = UUID.fromString(data.getUuid());
                    containerCache.invalidate(uuid);
                    containerCache.put(uuid, data.getPlayerData());
                });
    }
}
