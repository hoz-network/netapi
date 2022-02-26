package net.hoz.netapi.api

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

fun <T> Flux<T>.onErrorHandle(log: Logger): Flux<T> {
    return this.onErrorResume { ReactorHelper.fluxError(it, log) }
}

fun <T> Mono<T>.onErrorHandle(log: Logger): Mono<T> {
    return this.onErrorResume { ReactorHelper.monoError(it, log) }
}