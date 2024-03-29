/*
 * Copyright 2022 hoz-network.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.hoz.netapi.client.lang

import net.hoz.api.data.GameType
import org.screamingsandals.lib.lang.Translation

@Suppress("unused", "FunctionName") // suppressing useless inspections
object NLang {
    val NETWORK_PREFIX: Translation = Translation.of("network.prefix")

    val NETWORK_NAME: Translation = Translation.of("network.name")

    val COMMON_TRUE: Translation = Translation.of("common.true")
    val COMMON_FALSE: Translation = Translation.of("common.false")
    val COMMON_UNKNOWN: Translation = Translation.of("common.unknown")
    val COMMON_INTERNAL_ERROR: Translation = Translation.of("common.internal-error")
    val COMMON_REQUIRED: Translation = Translation.of("common.required")
    val COMMON_CLICK_TO_COPY: Translation = Translation.of("common.click-to-copy")

    fun GAME_PREFIX(gameType: GameType): Translation = Translation.of("games.${gameType.name.lowercase()}.prefix")

    const val COMMON_COMMANDS_INVALID_SYNTAX = "common.commands.invalid-syntax"
    const val COMMON_COMMANDS_INVALID_SENDER = "common.commands.invalid-sender"
    const val COMMON_COMMANDS_INVALID_ARGUMENT = "common.commands.invalid-argument"
    const val COMMON_COMMANDS_NO_PERMISSIONS = "common.commands.no-permissions"
}