package net.hoz.netapi.client.lang

import com.iamceph.resulter.core.Resultable
import com.iamceph.resulter.core.api.ResultStatus
import net.kyori.adventure.text.minimessage.Template
import org.screamingsandals.lib.lang.Translation

sealed interface LangResultable : Resultable {
    fun translations(): List<Translation>

    fun templates(): MutableList<Template>

    fun withTemplate(template: Template): LangResultable {
        templates().add(template)
        return this
    }

    companion object {
        fun ok(): LangResultable {
            return LangResultableImpl(ResultStatus.OK)
        }

        fun ok(translation: Translation): LangResultable {
            return LangResultableImpl(ResultStatus.OK, listOf(translation))
        }

        fun ok(translations: List<Translation>): LangResultable {
            return LangResultableImpl(ResultStatus.OK, translations)
        }

        fun fail(translation: Translation): LangResultable {
            return LangResultableImpl(ResultStatus.FAIL, listOf(translation))
        }

        fun fail(translations: List<Translation>): LangResultable? {
            return LangResultableImpl(ResultStatus.FAIL, translations)
        }

        fun fail(translations: Translation, throwable: Throwable): LangResultable {
            return LangResultableImpl(ResultStatus.OK, listOf(translations), throwable)
        }


        fun fail(translations: List<Translation>, throwable: Throwable): LangResultable {
            return LangResultableImpl(ResultStatus.OK, translations, throwable)
        }

        fun fail(throwable: Throwable): LangResultable {
            return LangResultableImpl(ResultStatus.OK, listOf(), throwable)
        }
    }
}