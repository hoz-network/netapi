package net.hoz.netapi.grpc.interceptor.client;

import io.grpc.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.hoz.netapi.grpc.interceptor.InterceptorKeys;

import java.util.UUID;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
@Getter
class ClientInterceptorImpl implements NetClientInterceptor {
    private final UUID clientId;
    private final String token;
    private UUID serverId;

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(final Listener<RespT> responseListener, final Metadata headers) {
                headers.put(InterceptorKeys.CLIENT_ID, clientId.toString());
                headers.put(InterceptorKeys.TOKEN, token);

                super.start(new ForwardingClientCallListener.SimpleForwardingClientCallListener<>(responseListener) {
                    @Override
                    public void onHeaders(Metadata headers) {
                        final var idString = headers.get(InterceptorKeys.SERVER_ID);

                        if (idString == null) {
                            throw new UnsupportedOperationException("SERVER_ID not found!");
                        }

                        serverId = UUID.fromString(idString);
                        super.onHeaders(headers);
                    }
                }, headers);
            }
        };
    }
}
