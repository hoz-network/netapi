package net.hoz.api.data;

import java.io.Serializable;

public interface Identifiable extends Serializable {

    static Identifiable of(String identifier) {
        return () -> identifier;
    }

    String getIdentifier();
}
