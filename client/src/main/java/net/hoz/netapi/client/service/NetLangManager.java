package net.hoz.netapi.client.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.Empty;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.data.DataOperation;
import net.hoz.api.data.ReactorHelper;
import net.hoz.api.service.LangData;
import net.hoz.api.service.NetLangServiceClient;
import net.hoz.netapi.client.config.ClientConfig;
import net.hoz.netapi.client.data.DataHolder;
import net.hoz.netapi.client.lang.NLang;
import net.hoz.netapi.client.lang.NetTranslationContainer;
import net.kyori.adventure.text.Component;
import org.apache.commons.lang.LocaleUtils;
import org.screamingsandals.lib.lang.Lang;
import org.screamingsandals.lib.lang.LangService;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.PlayerWrapper;
import org.screamingsandals.lib.sender.CommandSenderWrapper;
import org.screamingsandals.lib.utils.Controllable;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Locale;

@Slf4j
public class NetLangManager extends LangService implements Disposable {
    @Getter
    private static final Locale FALLBACK_LOCALE = Locale.ENGLISH;

    private final Cache<Locale, Component> cachedPrefixes = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    private final NetLangServiceClient langService;
    private final NetPlayerManager playerManager;
    private final ClientConfig clientConfig;

    private final Sinks.Many<LangData> updateSink;
    private Disposable updateListener;

    @Inject
    public NetLangManager(Controllable controllable,
                          NetLangServiceClient langService,
                          NetPlayerManager playerManager,
                          ClientConfig clientConfig,
                          Sinks.Many<LangData> updatesSink) {
        this.langService = langService;
        this.playerManager = playerManager;
        this.clientConfig = clientConfig;
        this.updateSink = updatesSink;

        Lang.initDefault(this);

        controllable.enable(this::subscribeToUpdates);
        controllable.preDisable(this::dispose);
    }

    @Override
    public Component resolvePrefix(CommandSenderWrapper senderWrapper) {
        final var lang = getSenderLocale(senderWrapper);
        return cachedPrefixes.get(lang, locale -> {
            if (clientConfig.origin() == DataOperation.OriginSource.GAME_SERVER) {
                return Message.of(NLang.GAME_PREFIX.apply(clientConfig.gameType()))
                        .getForJoined(senderWrapper);
            }
            return Message.of(NLang.NETWORK_PREFIX).getForJoined(senderWrapper);
        });
    }

    @Override
    protected Locale getSenderLocale(CommandSenderWrapper sender) {
        if (containers.isEmpty()) {
            log.warn("Language cannot be found, no TranslationContainer is available.");
        }

        if (sender.getType() == CommandSenderWrapper.Type.CONSOLE) {
            return FALLBACK_LOCALE;
        }

        final var player = sender.as(PlayerWrapper.class).getUuid();
        final var maybeCode = playerManager.getLangCode(player);
        if (maybeCode.isFail() || maybeCode.data().isEmpty()) {
            return super.getSenderLocale(sender);
        }

        return LocaleUtils.toLocale(maybeCode.data());
    }

    @Override
    public void dispose() {
        if (updateListener != null) {
            updateListener.dispose();
        }

        cachedPrefixes.invalidateAll();
    }

    private void register(LangData data) {
        Locale code;
        try {
            code = LocaleUtils.toLocale(data.getCode());
        } catch (Exception e) {
            log.warn("Locale {} is invalid! Using default: {}", data.getCode(), FALLBACK_LOCALE.getLanguage());
            return;
        }

        log.trace("Registering new language [{}] - [{}]!", code.getLanguage(), data.getCode());
        try {
            final var dataHolder = DataHolder.of(data.getData());
            final var languageHolder = new NetTranslationContainer(dataHolder, null);

            containers.put(code, languageHolder);
        } catch (Throwable e) {
            log.warn("Exception occurred while registering language [{}]!", code, e);
        }
        log.trace("Registered new language - [{}]", data.getCode());
    }

    private void update(LangData data) {
        final var code = LocaleUtils.toLocale(data.getCode());
        log.trace("Received language update for code [{}]", code);
        if (!containers.containsKey(code)) {
            register(data);

            updateSink.tryEmitNext(data);
            return;
        }

        final var container = (NetTranslationContainer) containers.get(code);
        container.getDataHolder().update(data.getData());
        cachedPrefixes.invalidate(code);

        updateSink.tryEmitNext(data);
    }

    private void loadLanguages() {
        langService.all(Empty.getDefaultInstance())
                .doOnNext(this::register)
                .doOnComplete(() -> {
                    log.trace("All languages initialized, verifying integrity..");
                    if (containers.containsKey(FALLBACK_LOCALE)) {
                        final var fallback = containers.get(FALLBACK_LOCALE);
                        if (fallback != null) {
                            log.trace("Found fallback container, setting it to others.");
                            this.fallbackContainer = fallback;

                            //we don't want stack overflow if the translation is not found :)
                            containers.entrySet()
                                    .stream()
                                    .filter(next -> !next.getKey().equals(FALLBACK_LOCALE))
                                    .peek(next -> log.trace("Setting fallback container for [{}]", next.getKey()))
                                    .forEach((entry) -> entry.getValue().setFallbackContainer(fallback));
                        }
                    }
                    log.trace("Language integrity check OK.");
                })
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();
    }

    private void subscribeToUpdates() {
        if (updateListener != null) {
            updateListener.dispose();
        }

        loadLanguages();
        updateListener = langService.subscribe(Empty.getDefaultInstance())
                .doOnNext(this::update)
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();
    }
}
