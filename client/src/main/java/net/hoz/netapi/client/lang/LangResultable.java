package net.hoz.netapi.client.lang;

import com.iamceph.resulter.core.Resultable;
import com.iamceph.resulter.core.api.ResultStatus;
import net.kyori.adventure.text.minimessage.Template;
import org.screamingsandals.lib.lang.Translation;

import java.util.List;

/**
 * A Resultable extension that provides {@link Translation} with the result.
 */
public interface LangResultable extends Resultable {

    List<Translation> translations();

    List<Template> templates();

    static LangResultable ok() {
        return LangResultableImpl.builder()
                .status(ResultStatus.OK)
                .build();
    }

    static LangResultable ok(Translation translation) {
        return LangResultableImpl.builder()
                .status(ResultStatus.OK)
                .translations(List.of(translation))
                .build();
    }

    static LangResultable ok(List<Translation> translations) {
        return LangResultableImpl.builder()
                .status(ResultStatus.OK)
                .translations(translations)
                .build();
    }

    static LangResultable fail(Translation translation) {
        return LangResultableImpl.builder()
                .status(ResultStatus.FAIL)
                .translations(List.of(translation))
                .build();
    }

    static LangResultable fail(List<Translation> translations) {
        return LangResultableImpl.builder()
                .status(ResultStatus.FAIL)
                .translations(translations)
                .build();
    }

    static LangResultable fail(Translation translations, Throwable throwable) {
        return LangResultableImpl.builder()
                .status(ResultStatus.FAIL)
                .translations(List.of(translations))
                .error(throwable)
                .build();
    }


    static LangResultable fail(List<Translation> translations, Throwable throwable) {
        return LangResultableImpl.builder()
                .status(ResultStatus.FAIL)
                .translations(translations)
                .error(throwable)
                .build();
    }

    static LangResultable fail(Throwable throwable) {
        return LangResultableImpl.builder()
                .status(ResultStatus.FAIL)
                .error(throwable)
                .build();
    }

    default LangResultable withTemplate(Template template) {
        templates().add(template);
        return this;
    }
}
