package net.hoz.netapi.client.config;

import net.hoz.api.data.DataOperation;
import net.hoz.api.data.GameType;

public record ClientConfig(DataOperation.OriginSource origin, GameType gameType) {
}
