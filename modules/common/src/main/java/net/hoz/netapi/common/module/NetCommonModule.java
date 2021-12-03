package net.hoz.netapi.common.module;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.AbstractModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.hoz.api.data.DataOperation;
import net.hoz.api.data.game.GameType;
import net.hoz.netapi.client.module.ClientModule;
import net.hoz.netapi.common.util.StoreHolderSerializer;
import org.screamingsandals.lib.world.WorldHolder;

@EqualsAndHashCode(callSuper = false)
@Data
public class NetCommonModule extends AbstractModule {
    private final GameType gameType;
    private final DataOperation.OriginSource source;

    @Override
    protected void configure() {
        bind(Gson.class).toInstance(new GsonBuilder()
                .registerTypeAdapter(WorldHolder.class, new WorldHolder.WorldHolderTypeAdapter())
                .create());
        bind(StoreHolderSerializer.class).asEagerSingleton();

        install(new ClientModule(gameType, source));
    }
}
