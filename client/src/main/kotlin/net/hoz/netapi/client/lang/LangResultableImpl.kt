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

import com.iamceph.resulter.core.Resultable.Convertor
import com.iamceph.resulter.core.api.ResultStatus
import com.iamceph.resulter.core.model.ProtoResultable
import com.iamceph.resulter.core.model.Resulters
import net.kyori.adventure.text.minimessage.Template
import org.screamingsandals.lib.lang.Translation

internal data class LangResultableImpl(
    val status: ResultStatus,
    val error: Throwable?,
    override val translations: MutableList<Translation> = mutableListOf(),
    override val templates: MutableList<Template> = mutableListOf()
) : LangResultable {
    constructor(status: ResultStatus) : this(status, null)

    constructor(status: ResultStatus, translations: List<Translation>) : this(
        status,
        null,
        translations.toMutableList(),
        mutableListOf()
    )

    constructor(status: ResultStatus, error: Throwable, translations: List<Translation>) : this(
        status,
        error,
        translations.toMutableList(),
        mutableListOf()
    )

    override fun withTemplate(template: Template): LangResultable {
        templates.add(template)
        return this
    }

    override fun asProto(): ProtoResultable = convertor().asProto()

    override fun status(): ResultStatus = status

    override fun message(): String = translations.map { it.keys }.joinToString { ", " }

    override fun error(): Throwable? = error

    override fun isOk(): Boolean = status == ResultStatus.OK

    override fun isFail(): Boolean = status == ResultStatus.FAIL

    override fun isWarning(): Boolean = status == ResultStatus.WARNING

    /**
     * @see com.iamceph.resulter.core.Resultable.convertor
     */
    override fun convertor(): Convertor = object : Convertor {
        override fun json(): String {
            return Resulters.convertor().json(this@LangResultableImpl)
        }

        override fun <K> convert(target: Class<K>): K {
            return Resulters.convertor().convert(this@LangResultableImpl, target)
        }
    }
}