package net.hoz.netapi.client.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.google.protobuf.Timestamp;
import com.iamceph.resulter.core.DataResultable;
import com.iamceph.resulter.core.Resultable;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.commons.DataOperation;
import net.hoz.api.data.ReactorHelper;
import net.hoz.api.data.currency.CurrencyContainer;
import net.hoz.api.data.currency.CurrencyOperation;
import net.hoz.api.data.currency.NetCurrency;
import net.hoz.api.service.currency.CurrencyContainerRequest;
import net.hoz.api.service.currency.CurrencyContainerResult;
import net.hoz.api.service.currency.CurrencyOperationRequest;
import net.hoz.api.service.currency.CurrencyServiceClient;
import org.screamingsandals.lib.utils.Controllable;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
public class CurrencyDataProvider {
    private final Cache<UUID, CurrencyContainer> containerCache = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();
    private final CurrencyServiceClient currencyService;
    private final DataOperation.OriginSource originSource;

    @Getter
    private final Sinks.Many<CurrencyContainer> currencyUpdates;

    private Disposable updatesListener;

    @Inject
    public CurrencyDataProvider(Controllable controllable,
                                CurrencyServiceClient currencyService,
                                DataOperation.OriginSource originSource,
                                Sinks.Many<CurrencyContainer> currencyUpdates) {
        this.currencyService = currencyService;
        this.originSource = originSource;
        this.currencyUpdates = currencyUpdates;

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
                        .map(DataResultable::ok)
                        .orElse(DataResultable.fail("No data found.")))
                .defaultIfEmpty(DataResultable.fail("test"));
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
                .switchIfEmpty(currencyService.oneFor(CurrencyContainerRequest.newBuilder()
                                .setUuid(uuid.toString())
                                .build())
                        .filter(ReactorHelper.filterResult(log))
                        .mapNotNull(CurrencyContainerResult::getContainer));
    }

    private Mono<Resultable> sendNewOperation(CurrencyOperationRequest request) {
        return currencyService
                .operation(request)
                .map(result -> Resultable.convert(result.getResult()));
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

        updatesListener = currencyService
                .subscribe(Empty.getDefaultInstance())
                .doOnNext(container -> {
                    containerCache.put(UUID.fromString(container.getOwnableData().getOwner()), container);
                    currencyUpdates.tryEmitNext(container);
                })
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();
    }
}
