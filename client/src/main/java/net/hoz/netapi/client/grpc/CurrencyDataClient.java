package net.hoz.netapi.client.grpc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.iamceph.resulter.core.DataResult;
import com.iamceph.resulter.core.SimpleResult;
import com.iamceph.resulter.core.api.DataResultable;
import com.iamceph.resulter.core.api.Resultable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.commons.DataOperation;
import net.hoz.api.data.currency.CurrencyContainer;
import net.hoz.api.data.currency.CurrencyOperation;
import net.hoz.api.data.currency.NetCurrency;
import net.hoz.api.service.currency.CurrencyContainerRequest;
import net.hoz.api.service.currency.CurrencyContainerResult;
import net.hoz.api.service.currency.CurrencyOperationRequest;
import net.hoz.api.service.currency.ReactorCurrencyServiceGrpc;
import net.hoz.netapi.grpc.service.GrpcStubService;
import net.hoz.netapi.grpc.util.ReactorHelper;
import org.screamingsandals.lib.utils.Controllable;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class CurrencyDataClient {
    private final Cache<UUID, CurrencyContainer> containerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();
    private final AtomicReference<ReactorCurrencyServiceGrpc.ReactorCurrencyServiceStub> stub;
    private final DataOperation.OriginSource originSource;

    @Getter
    private final Sinks.Many<CurrencyContainer> currencyUpdates;

    private Disposable updatesListener;

    @Inject
    public CurrencyDataClient(GrpcStubService stubService,
                              Controllable controllable,
                              DataOperation.OriginSource originSource,
                              Sinks.Many<CurrencyContainer> currencyUpdates) {
        this.originSource = originSource;
        this.currencyUpdates = currencyUpdates;

        final var holder = stubService.getHolder(ReactorCurrencyServiceGrpc.class);
        this.stub = holder.getStub(ReactorCurrencyServiceGrpc.ReactorCurrencyServiceStub.class);

        holder.getChannel().renewCallback(this::listenForUpdates);
        listenForUpdates();

        controllable.preDisable(() -> {
            if (updatesListener != null) {
                updatesListener.dispose();
            }
        });
    }

    public Mono<DataResultable<NetCurrency>> getCurrency(UUID uuid, NetCurrency.Type type) {
        return getContainer(uuid)
                .map(container -> container.getCurrenciesList()
                        .stream()
                        .filter(cur -> cur.getCurrencyType() == type)
                        .findFirst()
                        .map(DataResult::ok)
                        .orElse(DataResult.fail("No data found.")))
                .defaultIfEmpty(DataResult.fail("test"));
    }

    public Mono<Resultable> addToCurrency(UUID uuid,
                                          NetCurrency.Type currencyType,
                                          DataOperation.OriginType originType,
                                          long count) {
        return sendNewOperation(CurrencyOperationRequest.newBuilder()
                .setOperation(buildOperation(uuid, currencyType,
                        CurrencyOperation.Action.ADD, originType, count))
                .build());
    }

    public Mono<Resultable> removeFromCurrency(UUID uuid,
                                                 NetCurrency.Type currencyType,
                                                 DataOperation.OriginType originType,
                                                 long count) {
        return sendNewOperation(CurrencyOperationRequest.newBuilder()
                .setOperation(buildOperation(uuid, currencyType,
                        CurrencyOperation.Action.REMOVE, originType, count))
                .build());
    }

    public Mono<Resultable> setNewCurrencyCount(UUID uuid,
                                                  NetCurrency.Type currencyType,
                                                  DataOperation.OriginType originType,
                                                  long count) {
        return sendNewOperation(CurrencyOperationRequest.newBuilder()
                .setOperation(buildOperation(uuid, currencyType,
                        CurrencyOperation.Action.SET_ALL, originType, count))
                .build());
    }

    private Mono<CurrencyContainer> getContainer(UUID uuid) {
        return Mono.fromSupplier(() -> containerCache.getIfPresent(uuid))
                .switchIfEmpty(stub.get()
                        .oneFor(CurrencyContainerRequest.newBuilder()
                                .setUuid(uuid.toString())
                                .build())
                        .filter(next -> ReactorHelper.resultFilter("getContainer", next.getResult(), log))
                        .mapNotNull(CurrencyContainerResult::getContainer));
    }

    private Mono<Resultable> sendNewOperation(CurrencyOperationRequest request) {
        return stub.get()
                .operation(request)
                .map(result -> SimpleResult.convert(result.getResult()));
    }

    private CurrencyOperation buildOperation(UUID owner, NetCurrency.Type type,
                                             CurrencyOperation.Action action,
                                             DataOperation.OriginType originType,
                                             long count) {
        final var timestamp = Instant.now();
        return CurrencyOperation.newBuilder()
                .setAction(action)
                .setCurrencyType(type)
                .setOwnerId(owner.toString())
                .setToModifyCount(count)
                .setOperation(DataOperation.newBuilder()
                        .setState(DataOperation.State.PENDING)
                        .setOriginSource(originSource)
                        .setOriginType(originType)
                        .setCreatedDateTime(Timestamp.newBuilder()
                                .setNanos(timestamp.getNano())
                                .setSeconds(timestamp.getEpochSecond())
                                .build())
                        .setOperationId(UUID.randomUUID().toString()))
                .build();
    }

    private void listenForUpdates() {
        if (updatesListener != null) {
            updatesListener.dispose();
        }

        updatesListener = stub.get()
                .subscribe(Empty.getDefaultInstance())
                .doOnNext(container -> {
                    containerCache.put(UUID.fromString(container.getOwnableData().getOwner()), container);
                    currencyUpdates.tryEmitNext(container);
                })
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();
    }
}
