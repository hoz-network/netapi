package net.hoz.netapi.client.service;

import com.google.inject.Inject;
import net.hoz.netapi.grpc.service.DefaultGrpcStubService;
import net.hoz.netapi.grpc.service.GrpcChannelService;

public class InjectableGrpcStubService extends DefaultGrpcStubService {

    @Inject
    public InjectableGrpcStubService(GrpcChannelService channelService) {
        super(channelService);
    }
}
