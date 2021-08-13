package net.hoz.netapi.grpc.service;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import lombok.RequiredArgsConstructor;
import net.hoz.netapi.grpc.channel.DefaultGrpcChannel;
import net.hoz.netapi.grpc.channel.GrpcChannel;
import net.hoz.netapi.grpc.config.GrpcConfig;
import org.screamingsandals.lib.utils.executor.ExecutorProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ExecutorService;

/**
 * @author Frantisek Novosad (fnovosad@monetplus.cz)
 */
@RequiredArgsConstructor
public class DefaultGrpcChannelService implements GrpcChannelService {
    private final Logger log = LoggerFactory.getLogger(GrpcChannelService.class);
    private final Map<Class<?>, ExecutorService> executors = new HashMap<>();
    private final Multimap<Class<?>, GrpcChannel> channelMap = ArrayListMultimap.create();
    private final Random random = new Random();
    private final GrpcConfig config;

    @Override
    public GrpcChannel getChannel(Class<?> serviceClass) {
        if (channelMap.containsKey(serviceClass)) {
            return channelMap.get(serviceClass)
                    .stream()
                    .findFirst()
                    .orElseThrow();
        }

        if (!executors.containsKey(serviceClass)) {
            buildExecutor(serviceClass);
        }

        if (config.getServiceId() == null) {
            config.setServiceId(UUID.randomUUID());
        }

        Preconditions.checkNotNull(config.getAddress(), "grpc address");
        Preconditions.checkNotNull(config.getToken(), "grpc token");
        final var firstAddress = config.getAddress().get(0);
        final var firstToken = config.getToken().get(0);

        final var channel = buildChannel(firstAddress, firstToken, serviceClass, false);
        channelMap.put(serviceClass, channel);

        return channel;
    }

    @Override
    public List<GrpcChannel> getChannels() {
        return getChannels(Object.class);
    }

    @Override
    public List<GrpcChannel> getChannels(Class<?> serviceClass) {
        if (channelMap.containsKey(serviceClass)) {
            return List.copyOf(channelMap.get(serviceClass));
        }

        if (!executors.containsKey(serviceClass)) {
            buildExecutor(serviceClass);
        }

        if (config.getServiceId() == null) {
            config.setServiceId(UUID.randomUUID());
        }

        final var newChannels = config.toMap();
        newChannels.forEach((address, token) -> {
            final var channel = buildChannel(address, token, serviceClass, true);
            channelMap.put(serviceClass, channel);
        });

        return List.copyOf(channelMap.get(serviceClass));
    }

    @Override
    public GrpcChannel getFailover(Class<?> serviceClass) {
        return null;
    }

    @Override
    public GrpcChannel getChannel() {
        return getChannel(Object.class);
    }

    @Override
    public void destroy() {
        channelMap.values().forEach(GrpcChannel::destroy);
        executors.forEach((key, value) -> {
            log.debug("Shutting down Executor for service [{}]", key.getSimpleName());
            ExecutorProvider.destroyExecutor(value);
        });
    }

    private void buildExecutor(Class<?> serviceClass) {
        final var simpleName = serviceClass.getSimpleName()
                .replace("Grpc", "")
                .replace("Reactor", "");

        final var executor = ExecutorProvider.buildScheduledExecutor(config.getThreadsCount(), simpleName);
        executors.put(serviceClass, executor);
    }

    private GrpcChannel.Config buildBackupConfig(Class<?> serviceClass) {
        if (config.getAddress().size() < 2) {
            log.trace("Not enough configured gRPC servers to use backup config feature.");
            return null;
        }

        if (config.getAddress().size() == 2) {
            final var address = config.getAddress().get(1);
            final var token = config.getToken().get(1);

            log.trace("Building backup config for channel for {}", address);

            return new DefaultGrpcChannel.DefaultConfig(address, config.getServiceId(), token,
                    executors.get(serviceClass), config.getCheckTimeSeconds());
        }

        final var maxNumber = config.getAddress().size() - 1;
        var configNumber = random.nextInt(maxNumber);

        if (configNumber == 0) {
            configNumber =+ 1;
        }

        final var address = config.getAddress().get(configNumber);
        final var token = config.getToken().get(configNumber);

        log.trace("Building backup config for channel for {}", address);

        return new DefaultGrpcChannel.DefaultConfig(address, config.getServiceId(), token,
                executors.get(serviceClass), config.getCheckTimeSeconds());
    }

    private GrpcChannel buildChannel(String address, String token, Class<?> serviceClass, boolean isInMultiStub) {
        if (isInMultiStub) {
            return new DefaultGrpcChannel(serviceClass.getSimpleName()
                    .replace("Grpc", "")
                    .replace("Reactor", ""),
                    new DefaultGrpcChannel.DefaultConfig(
                            address, config.getServiceId(),
                            token, executors.get(serviceClass), config.getCheckTimeSeconds()), isInMultiStub);
        }

        return new DefaultGrpcChannel(serviceClass.getSimpleName()
                .replace("Grpc", "")
                .replace("Reactor", ""),
                new DefaultGrpcChannel.DefaultConfig(
                        address, config.getServiceId(),
                        token, executors.get(serviceClass), config.getCheckTimeSeconds()),
                buildBackupConfig(serviceClass), isInMultiStub);
    }
}
