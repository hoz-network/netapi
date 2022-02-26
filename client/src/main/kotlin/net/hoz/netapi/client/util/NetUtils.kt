package net.hoz.netapi.client.util

import com.iamceph.resulter.core.DataResultable
import org.apache.commons.lang.LocaleUtils
import java.util.*

class NetUtils {
    companion object {
        /**
         * Tries to resolve the Locale from locale code.
         * This method uses [LocaleUtils.toLocale].
         *
         * @param localeCode locale code
         * @return [DataResultable] of the operation.
         */
        fun resolveLocale(localeCode: String?): DataResultable<Locale> {
            return try {
                DataResultable.ok(LocaleUtils.toLocale(localeCode))
            } catch (e: Exception) {
                DataResultable.fail(e)
            }
        }
    }
}