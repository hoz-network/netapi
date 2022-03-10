package net.hoz.netapi.client.util

import com.iamceph.resulter.core.DataResultable
import com.iamceph.resulter.kotlin.dataResultable
import org.apache.commons.lang.LocaleUtils
import java.util.*

fun resolveLocale(localeCode: String?): DataResultable<Locale> = dataResultable { LocaleUtils.toLocale(localeCode) }