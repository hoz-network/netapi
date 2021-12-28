package net.hoz.netapi.client.lang;

import net.hoz.api.data.GameType;
import org.screamingsandals.lib.lang.Translation;

import java.util.Locale;
import java.util.function.Function;

public interface NLang {

    Translation NETWORK_PREFIX = Translation.of("network.prefix");
    Translation NETWORK_NAME = Translation.of("network.name");

    Translation COMMON_TRUE = Translation.of("common.true");
    Translation COMMON_FALSE = Translation.of("common.false");
    Translation COMMON_UNKNOWN = Translation.of("common.unknown");
    Translation COMMON_INTERNAL_ERROR = Translation.of("common.internal-error");
    Translation COMMON_REQUIRED = Translation.of("common.required");
    Translation COMMON_CLICK_TO_COPY = Translation.of("common.click-to-copy");

    Function<GameType, Translation> GAME_PREFIX = gameType ->
            Translation.of("games." + gameType.name().toLowerCase(Locale.ROOT) + ".prefix");

    String COMMON_COMMANDS_INVALID_SYNTAX = "common.commands.invalid-syntax";
    String COMMON_COMMANDS_INVALID_SENDER = "common.commands.invalid-sender";
    String COMMON_COMMANDS_INVALID_ARGUMENT = "common.commands.invalid-argument";
    String COMMON_COMMANDS_NO_PERMISSIONS = "common.commands.no-permissions";
}
