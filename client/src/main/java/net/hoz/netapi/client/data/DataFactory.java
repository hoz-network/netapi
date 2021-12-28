package net.hoz.netapi.client.data;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import lombok.NoArgsConstructor;
import org.screamingsandals.lib.world.WorldHolder;
import org.screamingsandals.lib.world.configurate.WorldHolderTypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializer;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;

import java.util.HashMap;
import java.util.Map;

//TODO: this should be probably removed.
@NoArgsConstructor
public final class DataFactory {
    private static final DataFactory DATA_FACTORY = new DataFactory();
    private TypeSerializerCollection typeSerializers = TypeSerializerCollection.defaults();
    private NetTypeFactory gsonFactory = new NetTypeFactory();

    static {
        add(WorldHolder.class, new WorldHolderTypeSerializer());
    }

    public static <T> void add(Class<T> tClass, TypeSerializer<T> serializer) {
        DATA_FACTORY.typeSerializers = DATA_FACTORY.typeSerializers.childBuilder().register(tClass, serializer).build();
    }

    public static TypeSerializerCollection getConfigurateSerializers() {
        return DATA_FACTORY.typeSerializers;
    }

    @SuppressWarnings("unchecked")
    protected static class NetTypeFactory implements TypeAdapterFactory {
        private final Map<TypeToken<?>, TypeAdapter<?>> adapters = new HashMap<>();

        public <T> void add(TypeToken<T> token, TypeAdapter<T> adapter) {
            adapters.put(token, adapter);
        }

        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            if (adapters.containsKey(type)) {
                return (TypeAdapter<T>) adapters.get(type);
            }
            return null;
        }
    }

}
