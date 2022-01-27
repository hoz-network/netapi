package net.hoz.netapi.client

import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.core.model.GrpcResultable
import reactor.core.publisher.Mono

fun Mono<GrpcResultable>.resultable() : Mono<Resultable> {
    return this.map { Resultable.convert(it) }
}