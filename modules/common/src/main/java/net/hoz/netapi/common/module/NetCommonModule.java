package net.hoz.netapi.common.module;


import com.google.inject.AbstractModule;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.hoz.netapi.client.config.ClientConfig;
import net.hoz.netapi.client.module.ClientModule;

@EqualsAndHashCode(callSuper = false)
@Data
public class NetCommonModule extends AbstractModule {
    private final ClientConfig clientConfig;

    @Override
    protected void configure() {
        install(new ClientModule(clientConfig));
    }
}
