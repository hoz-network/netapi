package net.hoz.netapi.client.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.inject.Inject;
import com.google.protobuf.Empty;
import com.iamceph.resulter.core.DataResultable;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.data.DataOperation;
import net.hoz.api.util.ReactorHelper;
import net.hoz.api.service.LangData;
import net.hoz.api.service.NetLangServiceClient;
import net.hoz.netapi.client.config.ClientConfig;
import net.hoz.netapi.client.data.DataHolder;
import net.hoz.netapi.client.lang.NLang;
import net.hoz.netapi.client.lang.NetTranslationContainer;
import net.hoz.netapi.client.util.NetUtils;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;
import org.screamingsandals.lib.lang.Lang;
import org.screamingsandals.lib.lang.LangService;
import org.screamingsandals.lib.lang.Message;
import org.screamingsandals.lib.player.PlayerWrapper;
import org.screamingsandals.lib.sender.CommandSenderWrapper;
import org.screamingsandals.lib.utils.Controllable;
import reactor.core.Disposable;
import reactor.core.publisher.Sinks.Many;

import java.time.Duration;
import java.util.Locale;
import java.util.UUID;

/**
 * Service that manages network languages per-player.
 * Prefixes for messages, texts and language in general for all players.
 * The data are retrieved from BAGR. It is also possible to listen to updates for the languages.
 */
@Slf4j
public class NetLangProvider extends LangService implements Disposable {
    /**
     * A fallback locale.
     */
    private static final Locale FALLBACK_LOCALE = Locale.ENGLISH;
    /**
     * Cache for per-locale prefixes. This is not player dependent.
     */
    private final Cache<Locale, Component> cachedPrefixes = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(30))
            .build();

    private final NetLangServiceClient langService;
    private final NetPlayerProvider playerManager;
    private final ClientConfig clientConfig;

    /**
     * A {@link Many} sink for new language data updates.
     */
    private final Many<LangData> updateSink;
    /**
     * Disposable for remote updates.
     */
    private Disposable updateListener;

    @Inject
    public NetLangProvider(NetLangServiceClient langService,
                           NetPlayerProvider playerManager,
                           ClientConfig clientConfig,
                           Many<LangData> updatesSink,
                           Controllable controllable) {
        this.langService = langService;
        this.playerManager = playerManager;
        this.clientConfig = clientConfig;
        this.updateSink = updatesSink;

        Lang.initDefault(this);

        controllable.enable(this::subscribeToUpdates);
        controllable.preDisable(this::dispose);
    }

    /**
     * Gets prefix for the sender.
     * Resolving of the prefix is dependent on the server type, for example
     * - if the server is a game server, the prefix will be resolved from the {@link net.hoz.api.data.GameType}
     * - if the server is a lobby server, network prefix will be used.
     *
     * @param sender for who to resolve the prefix
     * @return a {@link Component} - the actual prefix.
     */
    @Override
    public @NotNull Component resolvePrefix(@NotNull CommandSenderWrapper sender) {
        final var lang = resolveLocale(sender);
        //TODO: create prefix resolver
        return cachedPrefixes.get(lang, locale -> {
            if (clientConfig.origin() == DataOperation.OriginSource.GAME_SERVER) {
                return Message
                        .of(NLang.GAME_PREFIX.apply(clientConfig.gameType()))
                        .getForJoined(sender);
            }

            return Message
                    .of(NLang.NETWORK_PREFIX)
                    .getForJoined(sender);
        });
    }

    /**
     * Tries to resolve the locale from the BAGR backend.
     * <p>
     * First, we try to get the cached locale from {@link NetPlayerProvider#resolveLocale(UUID)},
     * that should be cached as soon as the player joins the server.
     * If that fails, we use the actual Minecraft client locale.
     * <p>
     * NOTE: if the sender is a console, FALLBACK_LOCALE will be used.
     *
     * @param sender for who to resolve the prefix
     * @return a locale of the sender.
     */
    @Override
    protected @NotNull Locale resolveLocale(CommandSenderWrapper sender) {
        if (sender.getType() == CommandSenderWrapper.Type.CONSOLE) {
            return FALLBACK_LOCALE;
        }

        final var playerId = sender.as(PlayerWrapper.class).getUuid();
        final var maybeLocale = playerManager.resolveLocale(playerId);
        if (maybeLocale.isFail()) {
            return super.resolveLocale(sender);
        }

        return maybeLocale.data();
    }

    @Override
    public void dispose() {
        if (updateListener != null) {
            updateListener.dispose();
        }

        cachedPrefixes.invalidateAll();
    }

    /**
     * Registering of the actual language data.
     *
     * @param data data to register
     */
    protected void registerData(LangData data) {
        final var localeCode = data.getCode();
        final var maybeLocale = NetUtils.resolveLocale(localeCode);
        if (maybeLocale.isFail()) {
            log.warn("Error registering locale[{}] - {}", localeCode, maybeLocale.message());
            return;
        }

        final var locale = maybeLocale.data();
        log.trace("Registering new language [{}] - [{}]!", locale.getLanguage(), localeCode);

        buildContainer(locale, data).ifOk(this::registerContainer);
    }

    /**
     * Processing of the received lang data update.
     *
     * @param data data to update
     */
    protected void updateData(LangData data) {
        final var localeCode = data.getCode();
        final var maybeLocale = NetUtils.resolveLocale(localeCode);
        if (maybeLocale.isFail()) {
            log.warn("Error registering locale[{}] - {}", localeCode, maybeLocale.message());
            return;
        }

        final var locale = maybeLocale.data();
        log.trace("Received language update for code [{}]", localeCode);
        if (!containers.containsKey(locale)) {
            registerData(data);

            updateSink.tryEmitNext(data);
            return;
        }

        final var container = (NetTranslationContainer) containers.get(locale);
        container.getDataHolder().update(data.getData());
        cachedPrefixes.invalidate(locale);

        updateSink.tryEmitNext(data);
    }

    /**
     * Builds a {@link NetTranslationContainer} from lang data
     *
     * @param locale   locale of the container
     * @param langData actual data
     * @return {@link DataResultable} of the operation.
     */
    private DataResultable<NetTranslationContainer> buildContainer(Locale locale, LangData langData) {
        return DataHolder.of(langData.getData())
                .map(dataHolder -> new NetTranslationContainer(locale, dataHolder));
    }

    /**
     * Tries to register given container.
     *
     * @param container container to register
     */
    private void registerContainer(NetTranslationContainer container) {
        final var locale = container.getLocale();
        log.trace("Trying to register new locale [{}] with container [{}].", locale, container);
        if (containers.containsKey(locale)) {
            log.trace("Container for locale [{}] is already registered!", locale);
            return;
        }

        containers.put(locale, container);
        log.trace("Registered new language - [{}]", locale.getLanguage());
    }

    /**
     * Tries to load all languages from the BAGR:
     */
    protected void loadLanguages() {
        langService.all(Empty.getDefaultInstance())
                .doOnNext(this::registerData)
                .doOnComplete(this::onLoadingComplete)
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();
    }

    protected void onLoadingComplete() {
        log.trace("All languages initialized, verifying integrity..");
        if (containers.containsKey(FALLBACK_LOCALE)) {
            final var fallback = containers.get(FALLBACK_LOCALE);
            if (fallback != null) {
                log.trace("Found fallback container, setting it to others.");
                this.fallbackContainer = fallback;

                containers.entrySet()
                        .stream()
                        .filter(next -> !next.getKey().equals(FALLBACK_LOCALE))
                        .peek(next -> log.trace("Setting fallback container for [{}]", next.getKey()))
                        .forEach((entry) -> entry.getValue().setFallbackContainer(fallback));
            }
        }
        log.trace("Language integrity check OK.");
    }

    private void subscribeToUpdates() {
        if (updateListener != null) {
            updateListener.dispose();
        }

        loadLanguages();
        updateListener = langService.subscribe(Empty.getDefaultInstance())
                .doOnNext(this::updateData)
                .onErrorResume(ex -> ReactorHelper.fluxError(ex, log))
                .subscribe();
    }
}
