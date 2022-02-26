package net.hoz.netapi.api

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun <T> fluxError(ex: Throwable, log: Logger): Flux<T> = Flux.empty<T>().also { log.warn("Exception: ${ex.message}", ex) }

fun <T> monoError(ex: Throwable, log: Logger): Mono<T> = Mono.empty<T>().also { log.warn("Exception: ${ex.message}", ex) }

fun <T> Flux<T>.onErrorHandle(log: Logger): Flux<T> = onErrorResume { fluxError(it, log) }

fun <T> Mono<T>.onErrorHandle(log: Logger): Mono<T> = onErrorResume { monoError(it, log) }