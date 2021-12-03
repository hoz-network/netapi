package net.hoz.netapi.common.util;

import com.google.inject.Inject;
import net.hoz.api.data.game.store.StoreHolder;
import net.hoz.api.data.storage.DataType;
import net.hoz.netapi.client.data.DataFactory;
import net.hoz.netapi.client.service.GameDataProvider;
import net.hoz.netapi.client.util.GsonProvider;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.time.Duration;

public class StoreHolderSerializer implements TypeSerializer<StoreHolder> {
    private final static String IDENTIFIER_FIELD = "identifier";
    private final GameDataProvider gameDataProvider;

    @Inject
    public StoreHolderSerializer(GameDataProvider gameDataProvider) {
        this.gameDataProvider = gameDataProvider;
        DataFactory.add(StoreHolder.class, this);
    }

    @Override
    public StoreHolder deserialize(Type type, ConfigurationNode node) throws SerializationException {
        if (node.node(IDENTIFIER_FIELD).empty()) {
            return null;
        }

        try {
            final var storeData = gameDataProvider.defaultDataIdentified(DataType.STORE, node.node(IDENTIFIER_FIELD).getString())
                    .block(Duration.ofSeconds(1));

            if (storeData == null) {
                throw new SerializationException("No data found for store identified with " + node.node(IDENTIFIER_FIELD));
            }

            return GsonProvider.getGson().fromJson(storeData.getData(), StoreHolder.class);
        } catch (Throwable t) {
            throw new SerializationException(t);
        }
    }

    @Override
    public void serialize(Type type, @Nullable StoreHolder storeHolder, ConfigurationNode node) throws SerializationException {
        if (storeHolder == null) {
            node.raw(null);
            return;
        }

        node.node(IDENTIFIER_FIELD).set(storeHolder.getIdentifier());
    }
}
