package net.hoz.netapi.client.lang

import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.core.api.ResultStatus
import net.kyori.adventure.text.minimessage.Template
import org.screamingsandals.lib.lang.Translation

sealed interface LangResultable : Resultable {
    val translations: List<Translation>
    val templates: List<Template>

    fun withTemplate(template: Template): LangResultable

    companion object {
        fun ok(): LangResultable = LangResultableImpl(ResultStatus.OK)

        fun ok(translation: Translation): LangResultable = LangResultableImpl(ResultStatus.OK, listOf(translation))

        fun ok(vararg translations: Translation): LangResultable = LangResultableImpl(ResultStatus.OK, translations.toList())

        fun ok(translations: List<Translation>): LangResultable = LangResultableImpl(ResultStatus.OK, translations)

        fun fail(translation: Translation): LangResultable = LangResultableImpl(ResultStatus.FAIL, listOf(translation))

        fun fail(translations: List<Translation>): LangResultable = LangResultableImpl(ResultStatus.FAIL, translations)

        fun fail(translations: Translation, throwable: Throwable): LangResultable = LangResultableImpl(ResultStatus.OK, throwable, listOf(translations))

        fun fail(translations: List<Translation>, throwable: Throwable): LangResultable = LangResultableImpl(ResultStatus.OK, throwable, translations)

        fun fail(throwable: Throwable): LangResultable = LangResultableImpl(ResultStatus.OK, throwable)
    }
}