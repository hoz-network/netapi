package net.hoz.netapi.client.util;

import com.google.protobuf.Any;
import com.google.protobuf.Message;
import com.iamceph.resulter.core.DataResultable;
import lombok.experimental.UtilityClass;

@UtilityClass
public class Unpacker {

    public <K extends Message> K unpackUnsafe(Any data, Class<K> type) {
        try {
            return data.unpack(type);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public <K extends Message> DataResultable<K> unpackSafe(Any data, Class<K> type) {
        try {
            return DataResultable.failIfNull(data.unpack(type));
        } catch (Exception e) {
            return DataResultable.fail(e);
        }
    }
}
