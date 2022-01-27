package net.hoz.netapi.api

import org.slf4j.Logger
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

class ReactorHelper {

    companion object {
        fun <T> fluxError(ex: Throwable, log: Logger): Flux<T> {
            log.warn("Exception: ${ex.message}", ex)
            return Flux.empty()
        }

        fun <T> monoError(ex: Throwable, log: Logger): Mono<T> {
            log.warn("Exception: ${ex.message}", ex)
            return Mono.empty()
        }
    }
}