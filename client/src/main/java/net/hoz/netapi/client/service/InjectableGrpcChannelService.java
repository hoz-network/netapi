package net.hoz.netapi.client.service;

import com.google.inject.Inject;
import net.hoz.netapi.grpc.config.GrpcConfig;
import net.hoz.netapi.grpc.service.DefaultGrpcChannelService;

public class InjectableGrpcChannelService extends DefaultGrpcChannelService {

    @Inject
    public InjectableGrpcChannelService(GrpcConfig config) {
        super(config);
    }
}
