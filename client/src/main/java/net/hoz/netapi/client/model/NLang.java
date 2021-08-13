package net.hoz.netapi.client.model;

import org.screamingsandals.lib.lang.Translation;

public interface NLang {

    Translation COMMON_TRUE = Translation.of("common.true");
    Translation COMMON_FALSE = Translation.of("common.false");
    Translation COMMON_UNKNOWN = Translation.of("common.unknown");
    Translation COMMON_INTERNAL_ERROR = Translation.of("common.internal-error");
    Translation COMMON_REQUIRED = Translation.of("common.required");
    Translation COMMON_CLICK_TO_COPY = Translation.of("common.click-to-copy");

    String COMMON_COMMANDS_INVALID_SYNTAX = "common.commands.invalid-syntax";
    String COMMON_COMMANDS_INVALID_SENDER = "common.commands.invalid-sender";
    String COMMON_COMMANDS_INVALID_ARGUMENT = "common.commands.invalid-argument";
    String COMMON_COMMANDS_NO_PERMISSIONS = "common.commands.no-permissions";
}
