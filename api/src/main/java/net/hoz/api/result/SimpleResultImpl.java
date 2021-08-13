package net.hoz.api.result;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import net.hoz.api.commons.Result;

@Builder(access = AccessLevel.PACKAGE)
@Getter
@ToString
public class SimpleResultImpl implements SimpleResult {
    private final Result.Status status;
    private final String message;
    private final Throwable throwable;

    @Override
    public boolean isOk() {
        return status == Result.Status.OK;
    }

    @Override
    public boolean isFail() {
        return status == Result.Status.FAIL;
    }
}
