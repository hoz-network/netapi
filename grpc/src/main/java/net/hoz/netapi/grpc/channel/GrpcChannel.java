package net.hoz.netapi.grpc.channel;

import com.iamceph.resulter.core.api.Resultable;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
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
     * @return checks channel's connection. Returns {@link com.iamceph.resulter.core.api.ResultStatus#FAIL} if no connection is available.
     */
    Resultable checkConnection();

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
