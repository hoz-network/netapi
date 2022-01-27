package net.hoz.netapi.client.config

import net.hoz.api.data.DataOperation
import net.hoz.api.data.GameType

data class DataConfig(
    val origin: DataOperation.OriginSource,
    val gameType: GameType
)
