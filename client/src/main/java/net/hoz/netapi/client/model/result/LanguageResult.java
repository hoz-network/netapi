package net.hoz.netapi.client.model.result;

import com.iamceph.resulter.core.api.ResultStatus;
import com.iamceph.resulter.core.api.Resultable;
import net.kyori.adventure.text.minimessage.Template;
import org.screamingsandals.lib.lang.Translation;

import java.util.List;

public interface LanguageResult extends Resultable {

    List<Translation> translations();

    List<Template> templates();

    static LanguageResult ok() {
        return LanguageResultImpl.builder()
                .status(ResultStatus.OK)
                .build();
    }

    static LanguageResult ok(Translation translation) {
        return LanguageResultImpl.builder()
                .status(ResultStatus.OK)
                .translations(List.of(translation))
                .build();
    }

    static LanguageResult ok(List<Translation> translations) {
        return LanguageResultImpl.builder()
                .status(ResultStatus.OK)
                .translations(translations)
                .build();
    }

    static LanguageResult fail(Translation translation) {
        return LanguageResultImpl.builder()
                .status(ResultStatus.FAIL)
                .translations(List.of(translation))
                .build();
    }

    static LanguageResult fail(List<Translation> translations) {
        return LanguageResultImpl.builder()
                .status(ResultStatus.FAIL)
                .translations(translations)
                .build();
    }

    static LanguageResult fail(Translation translations, Throwable throwable) {
        return LanguageResultImpl.builder()
                .status(ResultStatus.FAIL)
                .translations(List.of(translations))
                .error(throwable)
                .build();
    }


    static LanguageResult fail(List<Translation> translations, Throwable throwable) {
        return LanguageResultImpl.builder()
                .status(ResultStatus.FAIL)
                .translations(translations)
                .error(throwable)
                .build();
    }

    static LanguageResult fail(Throwable throwable) {
        return LanguageResultImpl.builder()
                .status(ResultStatus.FAIL)
                .error(throwable)
                .build();
    }

    default LanguageResult withTemplate(Template template) {
        templates().add(template);
        return this;
    }
}
