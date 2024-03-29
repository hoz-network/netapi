/*
 * Copyright 2022 hoz-network.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.hoz.netapi.client.provider

import com.github.benmanes.caffeine.cache.Caffeine
import com.google.protobuf.Empty
import com.iamceph.resulter.core.DataResultable
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import mu.KotlinLogging
import net.hoz.api.data.DataOperation
import net.hoz.api.service.LangData
import net.hoz.api.service.NetLangServiceGrpcKt
import net.hoz.netapi.api.Controlled
import net.hoz.netapi.api.onErrorHandle
import net.hoz.netapi.client.config.DataConfig
import net.hoz.netapi.client.data.DataHolder
import net.hoz.netapi.client.lang.NLang
import net.hoz.netapi.client.lang.NetTranslationContainer
import net.hoz.netapi.client.util.NetUtils
import org.apache.commons.lang3.LocaleUtils
import org.screamingsandals.lib.kotlin.unwrap
import org.screamingsandals.lib.lang.Lang
import org.screamingsandals.lib.lang.LangService
import org.screamingsandals.lib.lang.Message
import org.screamingsandals.lib.player.PlayerWrapper
import org.screamingsandals.lib.sender.CommandSenderWrapper
import org.screamingsandals.lib.spectator.Component
import java.time.Duration
import java.util.*
import javax.inject.Inject

private val log = KotlinLogging.logger {}

//TODO: clean up this mess
class NetLangProvider @Inject constructor(
    private val langService: NetLangServiceGrpcKt.NetLangServiceCoroutineStub,
    private val playerProvider: NetPlayerProvider,
    private val clientConfig: DataConfig,
    private val langUpdater: MutableSharedFlow<LangData>,
) : LangService(), Controlled {

    companion object {
        private val FALLBACK_LOCALE = Locale.ENGLISH
    }

    /**
     * Cache for per-locale prefixes. This is not player dependent.
     */
    private val cachedPrefixes = Caffeine.newBuilder()
        .expireAfterAccess(Duration.ofMinutes(30))
        .build<Locale, Component>()

    private var langUpdateJob: Job? = null

    init {
        Lang.initDefault(this)
    }

    override fun dispose() {
        langUpdateJob?.cancel("NetLangProvider is disposing.")
        cachedPrefixes.invalidateAll()
    }

    override suspend fun initialize() {
        loadLanguages()

        langUpdateJob = coroutineScope {
            launch {
                langService.subscribe(Empty.getDefaultInstance())
                    .onEach { updateData(it) }
                    .onErrorHandle(log)
                    .cancellable()
                    .collect()
            }
        }
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

            return@get Message.of(NLang.NETWORK_PREFIX)
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
        val maybeLocale = playerProvider.resolveLocale(playerId)

        return if (maybeLocale.isFail) {
            super.resolveLocale(sender)
        } else maybeLocale.data()
    }

    fun localeUpdater(): SharedFlow<LangData> = langUpdater

    /**
     * Registering of the actual language data.
     *
     * @param data data to register
     */
    private fun registerData(data: LangData) {
        val localeCode = data.code
        val locale = LocaleUtils.toLocale(localeCode)
        log.debug { "Registering new language[${locale.language} - locale[$locale]" }

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
            log.warn { "Error registering locale[$localeCode] - ${maybeLocale.message}" }
            return
        }

        val locale = maybeLocale.data()
        log.debug { "Received locale update[$localeCode]" }
        if (!containers.containsKey(locale)) {
            registerData(langData)
            langUpdater.tryEmit(langData)
            return
        }

        val container = containers[locale] as NetTranslationContainer
        container.dataHolder.update(langData.data)
        cachedPrefixes.invalidate(locale)
        langUpdater.tryEmit(langData)
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
    private suspend fun loadLanguages() {
        langService.all(Empty.getDefaultInstance())
            .onEach { registerData(it) }
            .onCompletion { onLoadingComplete() }
            .onErrorHandle(log)
            .collect()
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