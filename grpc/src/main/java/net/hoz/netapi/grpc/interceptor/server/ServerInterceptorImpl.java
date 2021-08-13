package net.hoz.netapi.grpc.interceptor.server;

import io.grpc.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.hoz.netapi.grpc.interceptor.InterceptorKeys;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
@Slf4j
class ServerInterceptorImpl implements NetServerInterceptor {
    private final UUID serverId;
    private final String token;
    private final List<UUID> clients = new LinkedList<>();

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata metadata, ServerCallHandler<ReqT, RespT> nextCall) {
        final var clientToken = metadata.get(InterceptorKeys.TOKEN);
        if (clientToken == null || !clientToken.equals(token)) {
            throw new StatusRuntimeException(Status.UNAUTHENTICATED);

        }

        clients.add(UUID.fromString(Objects.requireNonNull(metadata.get(InterceptorKeys.CLIENT_ID))));

        final var listener = nextCall.startCall(new ForwardingServerCall.SimpleForwardingServerCall<>(call) {
            @Override
            public void sendHeaders(Metadata headers) {
                if (!headers.containsKey(InterceptorKeys.SERVER_ID)) {
                    headers.put(InterceptorKeys.SERVER_ID, serverId.toString());
                }
                super.sendHeaders(headers);
            }
        }, metadata);

        return new ExceptionHandlingServerCallListener<>(listener, call, metadata);
    }

    private static class ExceptionHandlingServerCallListener<ReqT, RespT>
            extends ForwardingServerCallListener.SimpleForwardingServerCallListener<ReqT> {
        private final ServerCall<ReqT, RespT> serverCall;
        private final Metadata metadata;

        ExceptionHandlingServerCallListener(ServerCall.Listener<ReqT> listener, ServerCall<ReqT, RespT> serverCall,
                                            Metadata metadata) {
            super(listener);
            this.serverCall = serverCall;
            this.metadata = metadata;
        }

        @Override
        public void onHalfClose() {
            try {
                super.onHalfClose();
            } catch (RuntimeException ex) {
                handleException(ex, serverCall, metadata);
                throw ex;
            }
        }

        @Override
        public void onReady() {
            try {
                super.onReady();
            } catch (RuntimeException ex) {
                handleException(ex, serverCall, metadata);
                throw ex;
            }
        }

        private void handleException(RuntimeException exception, ServerCall<ReqT, RespT> serverCall, Metadata metadata) {
            log.warn("Exception! Metadata: {}", metadata.toString(), exception);
            if (exception instanceof IllegalArgumentException) {
                serverCall.close(Status.INVALID_ARGUMENT.withDescription(exception.getMessage()), metadata);
            } else {
                serverCall.close(Status.UNKNOWN, metadata);
            }
        }
    }
}
