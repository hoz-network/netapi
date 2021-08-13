package net.hoz.netapi.grpc.interceptor.server;

import io.grpc.ServerInterceptor;

import java.util.List;
import java.util.UUID;

public interface NetServerInterceptor extends ServerInterceptor {

    static NetServerInterceptor of(UUID serverId, String token) {
        return new ServerInterceptorImpl(serverId, token);
    }

    UUID getServerId();

    String getToken();

    List<UUID> getClients();
}
