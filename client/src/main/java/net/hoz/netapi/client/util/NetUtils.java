package net.hoz.netapi.client.util;

import com.iamceph.resulter.core.DataResultable;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang.LocaleUtils;

import java.util.Locale;

@UtilityClass
public class NetUtils {

    /**
     * Tries to resolve the Locale from locale code.
     * This method uses {@link LocaleUtils#toLocale(String)}.
     *
     * @param localeCode locale code
     * @return {@link DataResultable} of the operation.
     */
    public DataResultable<Locale> resolveLocale(String localeCode) {
        try {
            return DataResultable.ok(LocaleUtils.toLocale(localeCode));
        } catch (Exception e) {
            return DataResultable.fail(e);
        }
    }
}
