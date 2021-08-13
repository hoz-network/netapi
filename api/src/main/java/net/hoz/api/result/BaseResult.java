package net.hoz.api.result;



import net.hoz.api.commons.Result;

import java.io.Serializable;

public interface BaseResult extends Serializable {

    Result.Status getStatus();

    String getMessage();

    Throwable getThrowable();

    boolean isOk();

    boolean isFail();

    default Result convert() {
        final var message = getMessage();
        return switch (getStatus()) {
            case OK -> Result.newBuilder()
                    .setStatus(Result.Status.OK)
                    .setMessage(message != null ? message : "")
                    .build();
            case FAIL -> Result.newBuilder()
                    .setStatus(Result.Status.FAIL)
                    .setMessage(message != null ? message : "")
                    .build();
            default -> Result.newBuilder()
                    .setStatus(Result.Status.UNRECOGNIZED)
                    .setMessage(message != null ? message : "")
                    .build();
        };
    }
}
