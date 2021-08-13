package net.hoz.netapi.grpc.channel;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import net.hoz.api.result.SimpleResult;
import net.hoz.netapi.grpc.interceptor.client.NetClientInterceptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

public interface GrpcChannel {

    /**
     * @return identifier of this channel. Identifier is created on channel creation.
     */
    UUID getChannelId();

    /**
     * @return channel config.
     */
    Config getConfig();

    /**
     * @return gRPC's {@link ManagedChannel}
     */
    AtomicReference<ManagedChannel> getChannel();

    /**
     * Sends simple heartbeat to the server.
     * If failed, tries to reconnect.
     *
     * @return checks channel's connection. Returns {@link net.hoz.protofiles.commons.Result.Status#FAIL} if no connection is available.
     */
    SimpleResult checkConnection();

    /**
     * Creates new listener if channel is switched to new one
     *
     * @param callback new callback
     */
    void renewCallback(ChannelRenewCallback callback);

    void destroy();

    interface Config {

        String getAddress();

        String getToken();

        UUID getClientId();

        Optional<UUID> getServerId();

        NetClientInterceptor getClientInterceptor();

        List<ClientInterceptor> getInterceptors();

        Executor getExecutor();

        Integer getCheckTime();
    }

    @FunctionalInterface
    interface ChannelRenewCallback {
        void channelRenew();
    }
}
