package net.hoz.netapi.client.lang

import net.hoz.netapi.client.data.DataHolder
import org.screamingsandals.lib.lang.container.TranslationContainer
import org.spongepowered.configurate.ConfigurationNode
import java.util.*

class NetTranslationContainer(
    val locale: Locale,
    val dataHolder: DataHolder
) : TranslationContainer {
    private var fallbackContainer: TranslationContainer? = null

    override fun getNode(): ConfigurationNode {
        return dataHolder.root
    }

    override fun setNode(configurationNode: ConfigurationNode) {
        //do nothing
    }

    override fun setFallbackContainer(fallbackContainer: TranslationContainer) {
        this.fallbackContainer = fallbackContainer
    }

    override fun getFallbackContainer(): TranslationContainer? {
        return fallbackContainer
    }

    override fun translate(keys: Collection<String>): List<String> {
        val where = keys.joinToString { "." }
        val node = dataHolder.node(where)

        if (node.isList) {
            return node.childrenList()
                .mapNotNull { it.string }
        }

        if (!node.empty()) {
            return listOf(node.getString(""))
        }

        return if (fallbackContainer != null) fallbackContainer!!.translate(keys) else listOf()
    }

    override fun translate(vararg strings: String): List<String> {
        return translate(strings.toList())
    }

    override fun isEmpty(): Boolean {
        return dataHolder.root.empty()
                || fallbackContainer != null
                && fallbackContainer!!.isEmpty
    }
}