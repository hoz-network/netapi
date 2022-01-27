package net.hoz.netapi.client.lang

import com.iamceph.resulter.core.Resultable.Convertor
import com.iamceph.resulter.core.api.ResultStatus
import com.iamceph.resulter.core.model.Resulters
import net.kyori.adventure.text.minimessage.Template
import org.screamingsandals.lib.lang.Translation

internal data class LangResultableImpl(
    val status: ResultStatus,
    val translations: List<Translation> = mutableListOf(),
    val error: Throwable?
) : LangResultable {
    private val templates = mutableListOf<Template>()

    constructor(status: ResultStatus) : this(status, mutableListOf(), null)

    constructor(status: ResultStatus, translations: List<Translation>) : this(status, translations, null)

    override fun translations(): List<Translation> {
        return translations
    }

    override fun templates(): MutableList<Template> {
        return templates
    }

    override fun status(): ResultStatus {
        return status
    }

    override fun message(): String {
        return translations
            .map { it.keys }
            .joinToString { ", " }
    }

    override fun error(): Throwable? {
        return error
    }

    override fun isOk(): Boolean {
        return status == ResultStatus.OK
    }

    override fun isFail(): Boolean {
        return status == ResultStatus.FAIL
    }

    override fun isWarning(): Boolean {
        return status == ResultStatus.WARNING
    }

    /**
     * @see com.iamceph.resulter.core.Resultable.convertor
     */
    override fun convertor(): Convertor {
        return object : Convertor {
            override fun json(): String {
                return Resulters.convertor().json(this@LangResultableImpl)
            }

            override fun <K> convert(target: Class<K>): K {
                return Resulters.convertor().convert(this@LangResultableImpl, target)
            }
        }
    }
}