package net.hoz.netapi.client.data

import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.kotlin.dataResultable
import com.iamceph.resulter.kotlin.resultable
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.gson.GsonConfigurationLoader

class DataHolderImpl(json: String) : DataHolder {
    override var root: ConfigurationNode = build(json)

    override fun node(key: String): ConfigurationNode = root.node(key.split("\\."))

    override fun update(input: String): Resultable = resultable { root = build(input) }

    override fun json(): DataResultable<String> = dataResultable { GsonConfigurationLoader.builder().buildAndSaveString(root) }

    private fun build(input: String): ConfigurationNode = GsonConfigurationLoader.builder().buildAndLoadString(input)
}