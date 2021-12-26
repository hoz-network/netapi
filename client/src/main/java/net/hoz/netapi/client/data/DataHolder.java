package net.hoz.netapi.client.data;

import com.iamceph.resulter.core.DataResultable;
import com.iamceph.resulter.core.Resultable;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

public interface DataHolder {

    static DataResultable<DataHolder> of(String json) {
        try {
            final var holder = new DataHolderImpl(json);
            return DataResultable.ok(holder);
        } catch (ConfigurateException e) {
            return DataResultable.fail(e);
        }
    }

    /**
     * Main config node
     * @return Main config node
     */
    ConfigurationNode root();

    /**
     * Node with key
     * @param key key
     * @return node for the given key
     */
    ConfigurationNode node(String key);

    /**
     * Updates the root node with given data
     * @param json json input
     */
    Resultable update(String json);

    /**
     * Gets all the data to String
     * @return all data in json
     */
    DataResultable<String> get();
}
