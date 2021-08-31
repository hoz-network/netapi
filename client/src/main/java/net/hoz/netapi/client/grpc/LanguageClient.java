package net.hoz.netapi.client.grpc;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.Empty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.commons.DataOperation;
import net.hoz.api.commons.GameType;
import net.hoz.api.data.storage.LanguageDataContainer;
import net.hoz.api.service.language.ReactorLanguageServiceGrpc;
import net.hoz.netapi.client.data.DataHolder;
import net.hoz.netapi.client.model.NLang;
import net.hoz.netapi.client.model.NetTranslationContainer;
import net.hoz.netapi.grpc.service.GrpcStubService;
import net.hoz.netapi.grpc.util.ReactorHelper;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.LocaleUtils;
import org.screamingsandals.lib.lang.Lang;
import org.screamingsandals.lib.lang.LangService;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.PlayerWrapper;
import org.screamingsandals.lib.sender.CommandSenderWrapper;
import org.screamingsandals.lib.utils.Controllable;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
public class LanguageClient extends LangService {
    private final Cache<Locale, Component> cachedPrefixes = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();
    private final AtomicReference<ReactorLanguageServiceGrpc.ReactorLanguageServiceStub> stub;
    private final PlayerDataClient playerDataClient;
    private final DataOperation.OriginSource origin;
    private final GameType gameType;

    @Getter
    private final Sinks.Many<LanguageDataContainer> containerUpdates;

    @Setter
    @Getter
    private Locale FALLBACK_LOCALE = Locale.ENGLISH;

    private Disposable containerUpdatesListener;

    @Inject
    public LanguageClient(PlayerDataClient playerDataClient,
                          DataOperation.OriginSource origin,
                          GameType gameType,
                          Sinks.Many<LanguageDataContainer> containerUpdates,
                          GrpcStubService stubService,
                          Controllable controllable) {
        this.playerDataClient = playerDataClient;
        this.origin = origin;
        this.gameType = gameType;
        this.containerUpdates = containerUpdates;
        final var holder = stubService.getHolder(ReactorLanguageServiceGrpc.class);
        this.stub = holder.getStub(ReactorLanguageServiceGrpc.ReactorLanguageServiceStub.class);

        holder.getChannel().renewCallback(this::listenForUpdates);
        listenForUpdates();

        initialize();

        Lang.initDefault(this);

        controllable.preDisable(() -> {
            if (containerUpdatesListener != null) {
                containerUpdatesListener.dispose();
            }
        });
    }

    public Flux<LanguageDataContainer> allLanguages() {
        return stub.get()
                .all(Empty.getDefaultInstance())
                .doOnNext(next -> log.trace("Got language data for code {}!", next.getLanguageCode()));
    }

    @Override
    protected Locale getSenderLocale(CommandSenderWrapper sender) {
        if (containers.isEmpty()) {
            log.warn("Language cannot be found, no TranslationContainers are available.");
        }

        if (sender.getType() == CommandSenderWrapper.Type.CONSOLE) {
            return FALLBACK_LOCALE;
        }

        final var uuid = sender.as(PlayerWrapper.class).getUuid();
        final var maybeData = playerDataClient.getPlayerDataCached(uuid);
        if (maybeData.isFail()) {
            playerDataClient.getPlayerData(uuid)
                    .subscribeOn(Schedulers.boundedElastic())
                    .subscribe();

            log.trace("Player data not found, using default language.");
            return super.getSenderLocale(sender);
        }

        final var locale = maybeData.data().getSettings().getLocale();
        if (locale.isEmpty()) {
            return super.getSenderLocale(sender);
        }
        return LocaleUtils.toLocale(locale);
    }

    @Override
    public Component resolvePrefix(CommandSenderWrapper senderWrapper) {
        final var lang = getSenderLocale(senderWrapper);
        return cachedPrefixes.get(lang, locale -> {
            if (origin == DataOperation.OriginSource.GAME_SERVER) {
                return Message.of(NLang.GAME_PREFIX.apply(gameType)).getForJoined(senderWrapper);
            }
            return Message.of(NLang.NETWORK_PREFIX).getForJoined(senderWrapper);
        });
    }

    private void register(LanguageDataContainer data) {
        Locale code;
        try {
            code = LocaleUtils.toLocale(data.getLanguageCode());
        } catch (Exception e) {
            log.warn("Locale {} is invalid! Using default: {}", data.getLanguageCode(), FALLBACK_LOCALE.getLanguage());
            return;
        }

        log.trace("Registering new language {} - {}!", code.getLanguage(), data.getLanguageCode());
        try {
            final var dataHolder = DataHolder.of(data.getLanguageData());
            final var languageHolder = new NetTranslationContainer(dataHolder, null);

            containers.put(code, languageHolder);
        } catch (Throwable e) {
            log.warn("Exception occurred while registering language {}!", code, e);
        }
        log.trace("Language registered! {}", data.getLanguageCode());
    }

    private void update(LanguageDataContainer data) {
        final var code = LocaleUtils.toLocale(data.getLanguageCode());
        log.trace("Received language update for code [{}]", code);
        if (!containers.containsKey(code)) {
            register(data);
            return;
        }

        final var container = (NetTranslationContainer) containers.get(code);
        container.getDataHolder().update(data.getLanguageData());
        cachedPrefixes.invalidateAll();
    }

    private void initialize() {
        allLanguages()
                .doOnComplete(() -> {
                    log.debug("Completed language initializing!");
                    if (containers.containsKey(FALLBACK_LOCALE)) {
                        final var fallback = containers.get(FALLBACK_LOCALE);
                        if (fallback != null) {
                            this.fallbackContainer = fallback;

                            //we don't want stack overflow if the translation is not found :)
                            containers.entrySet()
                                    .stream()
                                    .filter(next -> !next.getKey().equals(FALLBACK_LOCALE))
                                    .forEach((entry) -> entry.getValue().setFallbackContainer(fallback));
                        }
                    }
                })
                .doOnError(ex -> log.trace("Ex!", ex))
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(this::register);
    }

    private void listenForUpdates() {
        if (containerUpdatesListener != null) {
            containerUpdatesListener.dispose();
        }

        containerUpdatesListener = stub.get()
                .subscribe(Empty.getDefaultInstance())
                .doOnNext(next -> {
                    this.update(next);
                    containerUpdates.tryEmitNext(next);
                })
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();
    }
}
