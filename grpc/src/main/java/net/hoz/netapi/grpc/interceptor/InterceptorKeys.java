package net.hoz.netapi.grpc.interceptor;

import io.grpc.Metadata;

public interface InterceptorKeys {
    Metadata.Key<String> TOKEN = Metadata.Key.of("token", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> CLIENT_ID = Metadata.Key.of("clientId", Metadata.ASCII_STRING_MARSHALLER);
    Metadata.Key<String> SERVER_ID = Metadata.Key.of("serverId", Metadata.ASCII_STRING_MARSHALLER);
}
