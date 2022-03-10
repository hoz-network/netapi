package net.hoz.netapi.client.data

import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.kotlin.dataResultable
import com.iamceph.resulter.kotlin.resultable
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.gson.GsonConfigurationLoader

internal data class DataHolderImpl(override var root: ConfigurationNode) : DataHolder {
    constructor(json: String) : this(GsonConfigurationLoader.builder().buildAndLoadString(json))

    override fun node(key: String): ConfigurationNode = root.node(key.split("\\."))

    override fun update(input: String): Resultable = resultable { root = GsonConfigurationLoader.builder().buildAndLoadString(input) }

    override fun json(): DataResultable<String> = dataResultable { GsonConfigurationLoader.builder().buildAndSaveString(root) }
}