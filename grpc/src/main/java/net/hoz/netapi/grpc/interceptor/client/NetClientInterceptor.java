package net.hoz.netapi.grpc.interceptor.client;

import io.grpc.ClientInterceptor;

import java.util.UUID;

public interface NetClientInterceptor extends ClientInterceptor {

    static NetClientInterceptor of(UUID clientId, String token) {
        return new ClientInterceptorImpl(clientId, token);
    }

    UUID getClientId();

    UUID getServerId();

    String getToken();
}


