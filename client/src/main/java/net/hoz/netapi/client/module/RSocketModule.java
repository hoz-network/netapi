package net.hoz.netapi.client.module;

import com.google.inject.AbstractModule;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.transport.netty.client.TcpClientTransport;
import lombok.extern.slf4j.Slf4j;
import net.hoz.api.data.DataOperation;
import net.hoz.api.service.GameServiceClient;
import net.hoz.api.service.NetLangServiceClient;
import net.hoz.api.service.NetPlayerServiceClient;
import net.hoz.netapi.client.config.DataConfig;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

import java.time.Duration;

//TODO
@Slf4j
public class RSocketModule extends AbstractModule {
    private final DataConfig clientConfig;

    public RSocketModule(DataConfig clientConfig) {
        this.clientConfig = clientConfig;
    }

    @Override
    protected void configure() {
        final var rSocket = buildClient()
                .onErrorResume(ex -> buildClient())
                .block();

        bind(NetPlayerServiceClient.class).toInstance(new NetPlayerServiceClient(rSocket));
        bind(NetLangServiceClient.class).toInstance(new NetLangServiceClient(rSocket));

        //TODO
        if (clientConfig.origin() == DataOperation.OriginSource.GAME_SERVER) {
            bind(GameServiceClient.class).toInstance(new GameServiceClient(rSocket));
        }
    }

    //TODO
    private Mono<RSocket> buildClient() {
        Resume resume =
                new Resume()
                        .sessionDuration(Duration.ofMinutes(5))
                        .retry(
                                Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                                        .doBeforeRetry(s -> log.info("Disconnected. Trying to resume...")));

        return RSocketConnector
                .create()
                .resume(resume)
                .reconnect(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(1))
                        .doAfterRetry(s -> log.info("Reconnected: {}", s))
                        .doBeforeRetry(s -> log.info("Reconnecting - {}", s)))
                .connect(TcpClientTransport.create(
                                TcpClient.create()
                                        .doOnResolveError((connection, throwable) -> log.info("Ex -> {}", connection, throwable))
                                        .port(7878)
                                        .doOnDisconnected(connection -> log.info("Disconnected"))
                        )
                );
    }
}
