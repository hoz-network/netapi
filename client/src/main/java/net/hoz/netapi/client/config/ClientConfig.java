package net.hoz.netapi.client.config;

import net.hoz.api.commons.GameType;
import net.hoz.api.data.DataOperation;

public record ClientConfig(DataOperation.OriginSource origin, GameType gameType) {
}
