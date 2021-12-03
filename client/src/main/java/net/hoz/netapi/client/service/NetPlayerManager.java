package net.hoz.netapi.client.service;

import com.iamceph.resulter.core.DataResultable;

import javax.inject.Inject;
import java.util.UUID;

public class NetPlayerManager {
    private final PlayerDataProvider playerDataProvider;

    @Inject
    public NetPlayerManager(PlayerDataProvider playerDataProvider) {
        this.playerDataProvider = playerDataProvider;
    }

    public DataResultable<String> getLangCode(UUID uuid) {
        final var data = playerDataProvider.getDataCached(uuid);
        if (data.isFail()) {
            return data.transform();
        }
        return DataResultable.ok(data.data().getSettings().getLocale());
    }
}
