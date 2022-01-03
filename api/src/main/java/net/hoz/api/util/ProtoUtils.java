package net.hoz.api.util;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class ProtoUtils {

    public String getOrEmpty(String input) {
        if (input == null) {
            return "";
        }
        return input;
    }

    public Boolean getOrEmpty(Boolean input) {
        if (input == null) {
            return false;
        }
        return input;
    }

    public Integer getOrEmpty(Integer input) {
        if (input == null) {
            return 0;
        }
        return input;
    }

    public Long getOrEmpty(Long input) {
        if (input == null) {
            return 0L;
        }
        return input;
    }

    public Float getOrEmpty(Float input) {
        if (input == null) {
            return 0F;
        }
        return input;
    }

    public Double getOrEmpty(Double input) {
        if (input == null) {
            return 0D;
        }
        return input;
    }
}
