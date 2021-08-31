package net.hoz.netapi.grpc.util;

import com.iamceph.resulter.core.SimpleResult;
import com.iamceph.resulter.core.model.Result;
import io.grpc.StatusRuntimeException;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@UtilityClass
public class ReactorHelper {
    public <T> Flux<T> fluxError(Throwable ex, Logger log) {
        if ((!(ex instanceof StatusRuntimeException)
                || !ex.getMessage().contains("Received Goaway"))
                && !ex.getMessage().contains("UNAVAILABLE")) {
            log.warn("Unknown exception happened in gRPC! Message: {}", ex.getMessage(), ex);
        } else {
            log.trace("Connection with gRPC server failed! Trying to reconnect..");
        }
        return Flux.empty();
    }

    public <T> Mono<T> monoError(Throwable ex, Logger log) {
        if ((!(ex instanceof StatusRuntimeException)
                || !ex.getMessage().contains("Received Goaway"))
                && !ex.getMessage().contains("UNAVAILABLE")) {
            log.warn("Unknown exception happened in gRPC! Message: {}", ex.getMessage(), ex);
        } else {
            log.trace("Connection with gRPC server failed! Trying to reconnect..");
        }
        return Mono.empty();
    }

    public boolean resultFilter(String action, Result result, Logger logger) {
        final var res = SimpleResult.convert(result);
        if (res.isFail()) {
            logger.warn("Result failed while doing {}, result: {}", action, res);
            return false;
        }
        return true;
    }
}