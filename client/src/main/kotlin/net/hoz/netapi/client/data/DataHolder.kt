package net.hoz.netapi.client.data

import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.kotlin.dataResultable
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode

interface DataHolder {
    /**
     * Main configuration node.
     */
    var root: ConfigurationNode

    companion object {
        @JvmStatic
        fun of(input: String): DataResultable<DataHolder> = dataResultable { DataHolderImpl(input) }
    }

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
}