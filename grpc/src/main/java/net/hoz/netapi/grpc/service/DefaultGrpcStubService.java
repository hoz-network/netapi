package net.hoz.netapi.grpc.service;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import io.grpc.Channel;
import io.grpc.stub.AbstractStub;
import lombok.RequiredArgsConstructor;
import net.hoz.netapi.grpc.channel.GrpcChannel;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Frantisek Novosad (fnovosad@monetplus.cz)
 */
public class DefaultGrpcStubService implements GrpcStubService {
    private final Multimap<Class<?>, StubHolder> stubHolders = ArrayListMultimap.create();
    private final Map<Class<?>, MultiStubHolder> multiStubHolders = new HashMap<>();
    private final GrpcChannelService channelService;

    public DefaultGrpcStubService(GrpcChannelService channelService) {
        this.channelService = channelService;
    }

    @Override
    public StubHolder getHolder(Class<?> serviceClass) {
        return getHolder(serviceClass, channelService.getChannel(serviceClass));
    }

    @Override
    public StubHolder getHolder(Class<?> serviceClass, GrpcChannel channel) {
        if (stubHolders.containsKey(serviceClass)) {
            final var holder = stubHolders.get(serviceClass)
                    .stream()
                    .findFirst()
                    .orElseThrow();
            if (holder.getChannel().equals(channel)) {
                return holder;
            }
        }

        final var holder = new StubHolderImpl(serviceClass, channel);
        stubHolders.put(serviceClass, holder);
        return holder;
    }

    @Override
    public MultiStubHolder getMultiHolder(Class<?> serviceClass) {
        if (multiStubHolders.containsKey(serviceClass)) {
            return multiStubHolders.get(serviceClass);
        }

        final var holder = buildMultiHolder(serviceClass);
        multiStubHolders.put(serviceClass, holder);

        return holder;
    }

    /**
     * Builds the MultiStub holder.
     * @param serviceClass gRPC service class
     * @return MultiStub
     */
    private MultiStubHolder buildMultiHolder(Class<?> serviceClass) {
        final var stubs = channelService.getChannels(serviceClass)
                .stream()
                .map(channel -> getHolder(serviceClass, channel))
                .collect(Collectors.toList());

        return new MultiStubHolderImpl(stubs);
    }

    @Override
    public void destroy() {
        stubHolders.clear();
        multiStubHolders.clear();
    }

    public static class StubHolderImpl implements StubHolder {
        private final Class<?> serviceClass;
        private final GrpcChannel channel;
        private AtomicReference<AbstractStub<?>> reactiveStub;

        public StubHolderImpl(Class<?> serviceClass, GrpcChannel channel) {
            this.serviceClass = serviceClass;
            this.channel = channel;

            channel.renewCallback(this::buildStub);
        }

        @Override
        public GrpcChannel getChannel() {
            return channel;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <M extends AbstractStub<M>> AtomicReference<M> getStub(Class<M> stub) {
            if (reactiveStub == null) {
                buildStub();
            }
            return (AtomicReference<M>) reactiveStub;
        }

        private void buildStub() {
            final var stubChannel = channel.getChannel().get();

            try {
                final var newStubMethod = serviceClass.getDeclaredMethod("newReactorStub", Channel.class);
                final var stub = (AbstractStub<?>) newStubMethod.invoke(serviceClass, stubChannel);

                if (reactiveStub != null) {
                    reactiveStub.set(stub);
                } else {
                    reactiveStub = new AtomicReference<>(stub);
                }
            } catch (Exception e) {
                throw new RuntimeException("This is totally wrong.", e);
            }
        }
    }

    @RequiredArgsConstructor
    public static class MultiStubHolderImpl implements MultiStubHolder {
        private final Random random = new Random();
        private final List<StubHolder> holders;

        @Override
        public List<StubHolder> getStubHolders() {
            return List.copyOf(holders);
        }

        @Override
        public Optional<StubHolder> getFirstAvailableHolder() {
            try {
                return Optional.of(holders.get(random.nextInt(holders.size() - 1)));
            } catch (Throwable ignored) {
                return Optional.empty();
            }
        }

        @Override
        public <M extends AbstractStub<M>> List<AtomicReference<M>> getStubs(Class<M> stubClass) {
            return holders.stream()
                    .map(holder -> holder.getStub(stubClass))
                    .collect(Collectors.toList());
        }
    }

}