package net.hoz.netapi.client.data

import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode

interface DataHolder {

    /**
     * Returns main configuration node.
     * @return [ConfigurationNode]
     */
    fun root(): ConfigurationNode

    /**
     * Node with key
     * @param key key
     * @return node for the given key
     */
    fun node(key: String) : ConfigurationNode

    /**
     * Updates the root node with given data
     * @param input json input
     */
    fun update(input: String): Resultable

    /**
     * Gets all the data to String
     * @return all data in json
     */
    fun json(): DataResultable<String>

    companion object {
        fun of(input: String): DataResultable<DataHolder> {
            return try {
                val holder = DataHolderImpl(input)
                DataResultable.ok(holder)
            } catch (e: ConfigurateException) {
                DataResultable.fail(e)
            }
        }
    }
}