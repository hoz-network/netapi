package net.hoz.netapi.client.lang;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import net.hoz.netapi.client.data.DataHolder;
import org.screamingsandals.lib.lang.container.TranslationContainer;
import org.spongepowered.configurate.ConfigurationNode;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class NetTranslationContainer implements TranslationContainer {
    private final DataHolder dataHolder;
    @Getter
    @Setter
    private TranslationContainer fallbackContainer;

    @Override
    public ConfigurationNode getNode() {
        return dataHolder.root();
    }

    @Override
    public void setNode(ConfigurationNode configurationNode) {
        //do nothing
    }

    @Override
    public List<String> translate(Collection<String> collection) {
        final var where = String.join(".", collection);
        final var node = dataHolder.node(where);
        if (node.isList()) {
            return node.childrenList()
                    .stream()
                    .map(ConfigurationNode::getString)
                    .collect(Collectors.toList());
        }
        if (!node.empty()) {
            return List.of(node.getString(""));
        }

        return fallbackContainer != null ? fallbackContainer.translate(collection) : List.of();
    }

    @Override
    public List<String> translate(String... strings) {
        return translate(Arrays.asList(strings));
    }

    @Override
    public boolean isEmpty() {
        return dataHolder.root().empty() || (fallbackContainer != null && fallbackContainer.isEmpty());
    }
}
