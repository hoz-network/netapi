package net.hoz.netapi.common.module;


import com.google.inject.AbstractModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.hoz.netapi.client.config.DataConfig;
import net.hoz.netapi.client.module.ClientModule;

@EqualsAndHashCode(callSuper = false)
@Data
public class NetCommonModule extends AbstractModule {
    private final DataConfig clientConfig;

    @Override
    protected void configure() {
        install(new ClientModule(clientConfig));
    }
}
