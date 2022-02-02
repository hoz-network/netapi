package net.hoz.netapi.client.provider

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.Empty
import com.iamceph.resulter.core.DataResultable
import net.hoz.api.data.DataOperation
import net.hoz.api.service.LangData
import net.hoz.api.service.NetLangServiceClient
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.ReactorHelper
import net.hoz.netapi.client.config.DataConfig
import net.hoz.netapi.client.data.DataHolder
import net.hoz.netapi.client.lang.NLang
import net.hoz.netapi.client.lang.NetTranslationContainer
import net.hoz.netapi.client.util.NetUtils
import net.kyori.adventure.text.Component
import org.screamingsandals.lib.kotlin.unwrap
import org.screamingsandals.lib.lang.Lang
import org.screamingsandals.lib.lang.LangService
import org.screamingsandals.lib.lang.Message
import org.screamingsandals.lib.player.PlayerWrapper
import org.screamingsandals.lib.sender.CommandSenderWrapper
import org.slf4j.LoggerFactory
import reactor.core.Disposable
import reactor.core.publisher.Sinks.Many
import java.time.Duration
import java.util.*
import javax.inject.Inject

class NetLangProvider
@Inject
constructor(
    private val langService: NetLangServiceClient,
    private val playerManager: NetPlayerProvider,
    private val clientConfig: DataConfig,
    private val updateSink: Many<LangData>
) : LangService(), Controlled {
    private val log = LoggerFactory.getLogger(javaClass)
    private val FALLBACK_LOCALE = Locale.ENGLISH
    private var updateListener: Disposable? = null

    /**
     * Cache for per-locale prefixes. This is not player dependent.
     */
    private val cachedPrefixes = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(30))
        .build<Locale, Component>()

    init {
        Lang.initDefault(this)
    }

    override fun dispose() {
        updateListener?.dispose()
        cachedPrefixes.invalidateAll()
    }

    override fun initialize() {
        updateListener?.dispose()

        loadLanguages()

        updateListener = langService.subscribe(Empty.getDefaultInstance())
            .doOnNext { updateData(it) }
            .onErrorResume { ReactorHelper.fluxError(it, log) }
            .subscribe()
    }

    /**
     * Gets prefix for the sender.
     * Resolving of the prefix is dependent on the server type, for example
     * - if the server is a game server, the prefix will be resolved from the [net.hoz.api.data.GameType]
     * - if the server is a lobby server, network prefix will be used.
     *
     * @param sender for who to resolve the prefix
     * @return a [Component] - the actual prefix.
     */
    override fun resolvePrefix(sender: CommandSenderWrapper): Component {
        val lang = resolveLocale(sender)

        //TODO: create prefix resolver
        return cachedPrefixes.get(lang) {
            if (clientConfig.origin === DataOperation.OriginSource.GAME_SERVER) {
                return@get Message
                    .of(NLang.GAME_PREFIX(clientConfig.gameType))
                    .getForJoined(sender)
            }

            return@get Message
                .of(NLang.NETWORK_PREFIX)
                .getForJoined(sender)
        }
    }

    /**
     * Tries to resolve the locale from the BAGR backend.
     *
     *
     * First, we try to get the cached locale from [NetPlayerProvider.resolveLocale],
     * that should be cached as soon as the player joins the server.
     * If that fails, we use the actual Minecraft client locale.
     *
     *
     * NOTE: if the sender is a console, FALLBACK_LOCALE will be used.
     *
     * @param sender for who to resolve the prefix
     * @return a locale of the sender.
     */
    override fun resolveLocale(sender: CommandSenderWrapper): Locale {
        if (sender.type == CommandSenderWrapper.Type.CONSOLE) {
            return FALLBACK_LOCALE
        }

        val playerId = sender.unwrap(PlayerWrapper::class).uuid
        val maybeLocale = playerManager.resolveLocale(playerId)

        return if (maybeLocale.isFail) {
            super.resolveLocale(sender)
        } else maybeLocale.data()
    }

    /**
     * Registering of the actual language data.
     *
     * @param data data to register
     */
    private fun registerData(data: LangData) {
        val localeCode = data.code
        val maybeLocale = NetUtils.resolveLocale(localeCode)
        if (maybeLocale.isFail) {
            log.warn("Error registering locale[{}] - {}", localeCode, maybeLocale.message())
            return
        }
        val locale = maybeLocale.data()
        log.trace("Registering new language [{}] - [{}]!", locale.language, localeCode)
        buildContainer(locale, data)
            .ifOk { registerContainer(it) }
    }

    /**
     * Processing of the received lang data update.
     *
     * @param langData data to update
     */
    private fun updateData(langData: LangData) {
        val localeCode = langData.code
        val maybeLocale = NetUtils.resolveLocale(localeCode)
        if (maybeLocale.isFail) {
            log.warn("Error registering locale[$localeCode] - ${maybeLocale.message()}")
            return
        }

        val locale = maybeLocale.data()
        log.trace("Received language update for code [$localeCode]")
        if (!containers.containsKey(locale)) {
            registerData(langData)
            updateSink.tryEmitNext(langData)
            return
        }

        val container = containers[locale] as NetTranslationContainer
        container.dataHolder.update(langData.data)
        cachedPrefixes.invalidate(locale)
        updateSink.tryEmitNext(langData)
    }

    /**
     * Builds a [NetTranslationContainer] from lang data
     *
     * @param locale   locale of the container
     * @param langData actual data
     * @return [DataResultable] of the operation.
     */
    private fun buildContainer(locale: Locale, langData: LangData): DataResultable<NetTranslationContainer> {
        return DataHolder.of(langData.data)
            .map { dataHolder -> NetTranslationContainer(locale, dataHolder) }
    }

    /**
     * Tries to register given container.
     *
     * @param container container to register
     */
    private fun registerContainer(container: NetTranslationContainer) {
        val locale = container.locale
        log.trace("Trying to register new locale [$locale] with container [$container].")
        if (containers.containsKey(locale)) {
            log.trace("Container for locale [$locale] is already registered!")
            return
        }

        containers[locale] = container
        log.trace("Registered new language - [${locale.language}]")
    }

    /**
     * Tries to load all languages from the BAGR:
     */
    private fun loadLanguages() {
        langService.all(Empty.getDefaultInstance())
            .doOnNext { registerData(it) }
            .doOnComplete { onLoadingComplete() }
            .onErrorResume { ReactorHelper.fluxError(it, log) }
            .subscribe()
    }

    private fun onLoadingComplete() {
        log.trace("All languages initialized, verifying integrity..")
        if (!containers.containsKey(FALLBACK_LOCALE)) {
            log.trace("Language integrity check OK.")
            return
        }

        val fallback = containers[FALLBACK_LOCALE]
        if (fallback != null) {
            log.trace("Found fallback container, setting it to others.")
            fallbackContainer = fallback

            containers.entries
                .filter { it.key != FALLBACK_LOCALE }
                .forEach {
                    log.trace("Setting fallback container for [${it.key}]")
                    it.value.fallbackContainer = fallback
                }
        }
        log.trace("Language integrity check OK.")
    }
}