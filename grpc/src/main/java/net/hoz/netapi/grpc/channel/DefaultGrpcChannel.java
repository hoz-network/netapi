package net.hoz.netapi.grpc.channel;

import com.iamceph.resulter.core.SimpleResult;
import com.iamceph.resulter.core.api.Resultable;
import io.grpc.ClientInterceptor;
import io.grpc.ConnectivityState;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.hoz.netapi.grpc.interceptor.client.NetClientInterceptor;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Getter
public class DefaultGrpcChannel implements GrpcChannel {
    private final List<ChannelRenewCallback> renewCallbacks = new LinkedList<>();

    private final AtomicBoolean firstTry = new AtomicBoolean(true);
    private final AtomicBoolean usingBackup = new AtomicBoolean(false);
    private final AtomicBoolean wasError = new AtomicBoolean(false);
    private final AtomicReference<Config> activeConfig;
    private final AtomicReference<ManagedChannel> channel;

    private final String owner;
    private final UUID channelId;
    private final Config config;
    private final Config backup;
    private final boolean isInMultiStub;

    private ManagedChannel mainChannelTesting;
    private ManagedChannel backupChannelTesting;

    public DefaultGrpcChannel(String owner, Config config, boolean isInMultiStub) {
        this(owner, config, null, isInMultiStub);
    }

    /**
     * Main constructor.
     * @param owner owner of this  channel (service name)
     * @param config main configuration for the channel
     * @param backup backup configuration for the channel
     * @param isInMultiStub if the channel is in multi-stub holder
     */
    public DefaultGrpcChannel(String owner, Config config, Config backup, boolean isInMultiStub) {
        this.owner = owner;
        this.isInMultiStub = isInMultiStub;
        this.channelId = UUID.randomUUID();
        this.config = config;
        this.backup = backup;
        this.channel = new AtomicReference<>();
        this.activeConfig = new AtomicReference<>(config);
        channel.set(buildChannel(config));

        buildCheckTask();
    }

    /**
     * Checks for the connection to gRPC server.
     * Steps:
     * -> If the connection is somehow broken, rebuild it back to main.
     * -> If using backup, check it. If backup is unavailable, switch back to main.
     * -> If main connection is broken, try to switch to backup if possible
     * -> If no attempt is successfully done, wait for next check
     * @return {@link Resultable} of this checking
     */
    @Override
    public Resultable checkConnection() {
        log.trace("Checking connection for service {}.", owner);
        //rebuild channel if its broken somehow
        if (channel.get() == null
                || channel.get().isShutdown()
                || channel.get().isTerminated()) {
            replaceChannel(buildChannel(config));
            wasError.set(true);
            return checkMainConnection();
        }

        if (wasError.get()) {
            replaceChannel(buildChannel(activeConfig.get()));
        }

        //if using backup, check main first, then backup
        if (usingBackup.get() && !isInMultiStub) {
            log.trace("Checking MAIN connecting even when using BACKUP in case that we can switch to MAIN!");
            final var mainChannelResult = checkMainConnection();
            if (mainChannelResult.isOk()) {
                log.trace("Switching back to main connection!");
                activeConfig.set(config);
                replaceChannel(buildChannel(config));
                usingBackup.set(false);
                return mainChannelResult;
            }

            final var result = checkBackupConnection();
            if (result.isOk()) {
                if (wasError.get()) {
                    replaceChannel(buildChannel(activeConfig.get()));
                    wasError.set(false);
                }
                return result;
            }

            if (result.isFail()) {
                log.trace("Backup became unavailable, switching back to main..");
                activeConfig.set(config);
                replaceChannel(buildChannel(config));
                usingBackup.set(false);
            }

            log.trace("Result of checking backup channel: {}", result.status());
        }

        final var result = checkMainConnection();
        if (result.isFail()) {
            if (backup != null && !isInMultiStub) {
                log.trace("Main connection is unavailable, switching to backup..");
                final var backupCheck = checkBackupConnection();

                if (backupCheck.isOk()) {
                    replaceChannel(buildChannel(backup));
                    log.trace("Switched to backup channel!");
                    activeConfig.set(backup);
                    usingBackup.set(true);
                    return backupCheck;
                }

                wasError.set(true);
                log.trace("Cannot switch to backup connection..");
                return SimpleResult.fail("Cannot switch to backup connection!");
            }

            wasError.set(true);
            log.trace("Main channel is unavailable, checking again in {} seconds", config.getCheckTime());
            return SimpleResult.fail("Main channel is unavailable.");
        }

        if (result.isOk() && wasError.get()) {
            replaceChannel(buildChannel(activeConfig.get()));
            wasError.set(false);
            return result;
        }

        return result;
    }

    @Override
    public void renewCallback(ChannelRenewCallback callback) {
        renewCallbacks.add(callback);
    }

    @Override
    public void destroy() {
        channel.get().shutdown();
    }

    private void buildCheckTask() {
        final var executor = (ScheduledExecutorService) config.getExecutor();
        checkMainConnection();
        checkBackupConnection();

        executor.scheduleAtFixedRate(this::checkConnection, 10, config.getCheckTime(), TimeUnit.SECONDS);
    }

    private Resultable checkMainConnection() {
        if (mainChannelTesting == null) {
            mainChannelTesting = buildChannel(config);
        }

        return checkConnectionForChannel(mainChannelTesting, "main");
    }

    private Resultable checkBackupConnection() {
        if (backup == null) {
            return SimpleResult.fail("Backup not configured!");
        }

        if (backupChannelTesting == null) {
            backupChannelTesting = buildChannel(backup);
        }

        return checkConnectionForChannel(backupChannelTesting, "backup");
    }

    private Resultable checkConnectionForChannel(ManagedChannel channel, String type) {
        final var result = checkConnectionState(channel);
        if (result.isOk()) {
            //log.trace("OK!");
            return SimpleResult.ok();
        }

        log.trace("First attempt failed, trying again.");

        final var result2 = checkConnectionState(channel);
        if (result2.isOk()) {
            return SimpleResult.ok();
        }

        log.trace("Second attempt failed, trying again.");

        final var result3 = checkConnectionState(channel);
        if (result3.isOk()) {
            return SimpleResult.ok();
        }

        log.trace(result3.message());
        return result3;
    }

    private Resultable checkConnectionState(ManagedChannel channel) {
        final var connectState = channel.getState(true);
        if (connectState == ConnectivityState.READY) {
            return SimpleResult.ok();
        }
        return SimpleResult.fail("Channel is in wrong state: " + connectState.name());
    }

    private void replaceChannel(ManagedChannel channel) {
        final var oldChannel = this.channel.get();
        if (oldChannel != null) {
            log.trace("Shutting down old channel");
            oldChannel.shutdownNow();
        }

        log.trace("Replacing channel..");
        this.channel.set(channel);
        callCallback();
    }

    private ManagedChannel buildChannel(Config config) {
        //TODO: add keep-alive stuff
        //TODO: ssl?
        return ManagedChannelBuilder
                .forTarget(config.getAddress())
                .usePlaintext()
                .intercept(config.getInterceptors().toArray(new ClientInterceptor[0]))
                .intercept(config.getClientInterceptor())
                .executor(config.getExecutor())
                .build();
    }

    private void callCallback() {
        log.trace("Calling renew callbacks, size: {}", renewCallbacks.size());
        renewCallbacks.forEach(ChannelRenewCallback::channelRenew);
    }

    @Getter
    public static class DefaultConfig implements Config {
        private final List<ClientInterceptor> interceptors = new LinkedList<>();
        private final String address;
        private final UUID clientId;
        private final String token;
        private final Executor executor;
        private final Integer checkTime;

        private final NetClientInterceptor clientInterceptor;

        public DefaultConfig(String address, UUID clientId, String token,
                             Executor executor, Integer checkTime) {
            this.address = address;
            this.clientId = clientId;
            this.token = token;
            this.executor = executor;
            this.checkTime = checkTime;
            this.clientInterceptor = NetClientInterceptor.of(clientId, token);
        }

        @Override
        public Optional<UUID> getServerId() {
            if (clientInterceptor.getServerId() == null) {
                return Optional.empty();
            }

            return Optional.of(clientInterceptor.getServerId());
        }
    }
}
