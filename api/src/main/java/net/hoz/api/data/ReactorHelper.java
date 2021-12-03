package net.hoz.api.data;

import com.google.protobuf.GeneratedMessageV3;
import com.iamceph.resulter.core.Resultable;
import com.iamceph.resulter.core.model.GrpcResultable;
import lombok.experimental.UtilityClass;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Predicate;

@UtilityClass
public class ReactorHelper {
    /**
     * Handles the given exception and returns {@link Flux#empty()};
     *
     * @param ex  exception to handle
     * @param log logger to log
     * @param <T> type of the Flux
     * @return {@link Flux#empty()}
     */
    public <T> Flux<T> fluxError(Throwable ex, Logger log) {
        handle(ex, log);
        return Flux.empty();
    }

    /**
     * Handles the given exception and returns {@link Mono#empty()};
     *
     * @param ex  exception to handle
     * @param log logger to log
     * @param <T> type of the Mono
     * @return {@link Mono#empty()}
     */
    public <T> Mono<T> monoError(Throwable ex, Logger log) {
        handle(ex, log);
        return Mono.empty();
    }

    /**
     * Determines type of the error and logs what was wrong. Dummy consumer.
     *
     * @param ex  exception to handle
     * @param log logger to log
     */
    public void handle(Throwable ex, Logger log) {
        log.warn("Exception in RSocket! {}", ex.getMessage(), ex);
    }

    /**
     * Tries to filter the result. OK only if the result is OK or not found at all.
     *
     * @param log logger
     * @param <T> type
     * @return Predicate
     */
    public <T extends GeneratedMessageV3> Predicate<T> filterResult(Logger log) {
        return t -> {
            try {
                final var descriptor = t.getDescriptorForType().findFieldByName("result");
                final var result = t.getField(descriptor);

                if (result == null || result.getClass().isInstance(GrpcResultable.class)) {
                    log.warn("Result was not found in message - {} - {}", t.getClass().getSimpleName(), t);
                    return true;
                }

                final var resultable = Resultable.convert(result);
                if (resultable.isFail() || resultable.isWarning()) {
                    log.warn("Resultable is not OK -> {}", resultable);
                    return false;
                }
                return true;
            } catch (Exception e) {
                log.warn("Cannot find Result in message - {} - {}", t.getClass().getSimpleName(), t);
                return true;
            }
        };
    }
}
