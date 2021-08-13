package net.hoz.netapi.client.data;

import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;

public interface DataHolder {

    static DataHolder of(String json) throws ConfigurateException {
        return new DataHolderImpl(json);
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
    void update(String json);

    /**
     * Gets all the data to String
     * @return
     */
    String get();
}
