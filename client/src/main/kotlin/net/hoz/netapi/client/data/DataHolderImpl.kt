package net.hoz.netapi.client.data

import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.core.Resultable
import org.spongepowered.configurate.ConfigurateException
import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.gson.GsonConfigurationLoader

class DataHolderImpl(json: String) : DataHolder {
    private var root: ConfigurationNode

    init {
        this.root = build(json)
    }

    override fun root(): ConfigurationNode {
        return root
    }

    override fun node(key: String): ConfigurationNode {
        return root.node(key.split("\\."))
    }

    override fun update(input: String): Resultable {
        return try {
            this.root = build(input)
            Resultable.ok()
        } catch (e: ConfigurateException) {
            Resultable.fail(e)
        }
    }

    override fun json(): DataResultable<String> {
        return try {
            DataResultable.failIfNull(GsonConfigurationLoader.builder().buildAndSaveString(root))
        } catch (e: Exception) {
            DataResultable.fail(e)
        }
    }

    private fun build(input: String): ConfigurationNode {
        return GsonConfigurationLoader
            .builder()
            .buildAndLoadString(input)
    }
}