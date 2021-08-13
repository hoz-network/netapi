package net.hoz.netapi.grpc.service;

import io.grpc.stub.AbstractStub;
import net.hoz.netapi.grpc.channel.GrpcChannel;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Frantisek Novosad (fnovosad@monetplus.cz)
 */
public interface GrpcStubService {

    /**
     * Creates StubHolder for given service.
     *
     * @param serviceClass main gRPC service class
     * @return new StubHolder
     */
    StubHolder getHolder(Class<?> serviceClass);

    /**
     * Creates StubHolder for given service and channel
     *
     * @param serviceClass main gRPC service class
     * @param channel      channel
     * @return new StubHolder
     */
    StubHolder getHolder(Class<?> serviceClass, GrpcChannel channel);

    /**
     * @param serviceClass main gRPC service class
     * @return MultiStub holder for given class
     */
    MultiStubHolder getMultiHolder(Class<?> serviceClass);

    void destroy();

    /**
     * Holds reference of the gRPC stub and updates the stub in case of channel update
     */
    interface StubHolder {
        /**
         * @return active channel for this stub holder
         */
        GrpcChannel getChannel();

        /**
         * gRPC stub reference. This reference is changed when switching channels.
         * Not good to cache at all!!
         *
         * @param stubClass generated stub class (SomeServiceGrpc.SomeServiceGrpcStub)
         * @param <M>       type of the stub
         * @return reference of the stub
         */
        <M extends AbstractStub<M>> AtomicReference<M> getStub(Class<M> stubClass);
    }

    /**
     * Holds multiple stubs. That's it. :)
     */
    interface MultiStubHolder {
        /**
         * @return all available stubs
         */
        List<StubHolder> getStubHolders();

        /**
         * @return first available stub.
         */
        Optional<StubHolder> getFirstAvailableHolder();

        /**
         * This {@link List} contains all stub references from all holders.
         * See {@link StubHolder#getStub(Class)}
         * Not good to cache at all!!
         *
         * @param stubClass generated stub class (SomeServiceGrpc.SomeServiceGrpcStub)
         * @param <M>       type of the stub
         * @return reference of the stub
         */
        <M extends AbstractStub<M>> List<AtomicReference<M>> getStubs(Class<M> stubClass);
    }
}