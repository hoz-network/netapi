package net.hoz.netapi.client.module

import com.google.inject.AbstractModule
import io.rsocket.RSocket
import io.rsocket.core.RSocketConnector
import io.rsocket.core.Resume
import io.rsocket.transport.netty.client.TcpClientTransport
import net.hoz.api.data.DataOperation
import net.hoz.api.service.NetGameServiceClient
import net.hoz.api.service.NetLangServiceClient
import net.hoz.api.service.NetPlayerServiceClient
import net.hoz.netapi.client.config.DataConfig
import org.slf4j.LoggerFactory
import reactor.core.publisher.Mono
import reactor.netty.tcp.TcpClient
import reactor.util.retry.Retry
import java.time.Duration

class RSocketModule(private val config: DataConfig) : AbstractModule() {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun configure() {
        val rSocket = buildClient()
            .onErrorResume { buildClient() }
            .block()

        bind(NetPlayerServiceClient::class.java).toInstance(NetPlayerServiceClient(rSocket))
        bind(NetLangServiceClient::class.java).toInstance(NetLangServiceClient(rSocket))

        //TODO

        if (config.origin == DataOperation.OriginSource.GAME_SERVER) {
            bind(NetGameServiceClient::class.java).toInstance(NetGameServiceClient(rSocket))
        }
    }

    //TODO
    private fun buildClient(): Mono<RSocket> {
        val resume = Resume()
            .sessionDuration(Duration.ofMinutes(5))
            .retry(
                Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                    .doBeforeRetry { log.info("Disconnected. Trying to resume...") })

        return RSocketConnector
            .create()
            .resume(resume)
            .reconnect(
                Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                    .doAfterRetry { log.info("Reconnected: $it") }
                    .doBeforeRetry { log.info("Reconnecting: $it") }
            )
            .connect(TcpClientTransport.create(
                TcpClient.create()
                    .doOnResolveError { connection, ex ->
                        log.warn("Resolve error: $connection", ex)
                    }
                    .port(7878)
                    .doOnDisconnected { log.info("Disconnected.") })
            )
    }
}