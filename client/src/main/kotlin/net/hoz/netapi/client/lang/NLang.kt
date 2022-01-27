package net.hoz.netapi.client.lang

import net.hoz.api.data.GameType
import org.screamingsandals.lib.lang.Translation

interface NLang {
    companion object {
        val NETWORK_PREFIX = Translation.of("network.prefix")

        val NETWORK_NAME = Translation.of("network.name")

        val COMMON_TRUE = Translation.of("common.true")
        val COMMON_FALSE = Translation.of("common.false")
        val COMMON_UNKNOWN = Translation.of("common.unknown")
        val COMMON_INTERNAL_ERROR = Translation.of("common.internal-error")
        val COMMON_REQUIRED = Translation.of("common.required")
        val COMMON_CLICK_TO_COPY = Translation.of("common.click-to-copy")

        fun GAME_PREFIX(gameType: GameType): Translation {
            return Translation.of("games." + gameType.name.lowercase() + ".prefix")
        }

        val COMMON_COMMANDS_INVALID_SYNTAX = "common.commands.invalid-syntax"
        val COMMON_COMMANDS_INVALID_SENDER = "common.commands.invalid-sender"
        val COMMON_COMMANDS_INVALID_ARGUMENT = "common.commands.invalid-argument"
        val COMMON_COMMANDS_NO_PERMISSIONS = "common.commands.no-permissions"
    }
}