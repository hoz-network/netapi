package net.hoz.netapi.client.data;

import com.iamceph.resulter.core.DataResultable;
import com.iamceph.resulter.core.Resultable;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.gson.GsonConfigurationLoader;

@Slf4j
@ToString
final class DataHolderImpl implements DataHolder {
    private ConfigurationNode root;

    DataHolderImpl(String json) throws ConfigurateException {
        this.root = build(json);
    }

    @Override
    public ConfigurationNode root() {
        return root;
    }

    @Override
    public ConfigurationNode node(String key) {
        if (key == null) {
            log.trace("Something tried to get null node!");
            return CommentedConfigurationNode.root();
        }

        return root.node((Object[]) key.split("\\."));
    }

    @Override
    public Resultable update(String json) {
        try {
            this.root = build(json);
            return Resultable.ok();
        } catch (ConfigurateException e) {
            return Resultable.fail(e);
        }
    }

    @Override
    public DataResultable<String> get() {
        try {
            final var data = GsonConfigurationLoader.builder()
                    .defaultOptions(options -> options.serializers(builder ->
                            builder.registerAll(DataFactory.getConfigurateSerializers())))
                    .buildAndSaveString(root);
            return DataResultable.ok(data);
        } catch (Exception e) {
            return DataResultable.fail(e);
        }
    }

    private ConfigurationNode build(String json) throws ConfigurateException {
        return GsonConfigurationLoader.builder()
                .defaultOptions(options -> options.serializers(builder ->
                        builder.registerAll(DataFactory.getConfigurateSerializers())))
                .buildAndLoadString(json);
    }
}
