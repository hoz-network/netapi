package net.hoz.netapi.grpc.service;


import net.hoz.netapi.grpc.channel.GrpcChannel;

import java.util.List;

public interface GrpcChannelService {

    /**
     * @return single channel holder that is configured
     */
    GrpcChannel getChannel();

    GrpcChannel getChannel(Class<?> serviceClass);

    List<GrpcChannel> getChannels();

    List<GrpcChannel> getChannels(Class<?> serviceClass);

    GrpcChannel getFailover(Class<?> serviceClass);

    void destroy();
}
