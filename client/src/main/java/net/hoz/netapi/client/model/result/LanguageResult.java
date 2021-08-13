package net.hoz.netapi.client.model.result;

import net.hoz.api.commons.Result;
import net.hoz.api.result.BaseResult;
import net.hoz.api.result.DataResult;
import net.hoz.api.result.SimpleResult;
import net.kyori.adventure.text.minimessage.Template;
import org.screamingsandals.lib.lang.Translation;

import java.util.List;

public interface LanguageResult extends BaseResult {

    List<Translation> getTranslations();

    static LanguageResult ok() {
        return LanguageResultImpl.builder()
                .status(Result.Status.OK)
                .build();
    }

    static LanguageResult ok(Translation translation) {
        return LanguageResultImpl.builder()
                .status(Result.Status.OK)
                .translations(List.of(translation))
                .build();
    }

    static LanguageResult ok(List<Translation> translations) {
        return LanguageResultImpl.builder()
                .status(Result.Status.OK)
                .translations(translations)
                .build();
    }

    static LanguageResult fail(Translation translation) {
        return LanguageResultImpl.builder()
                .status(Result.Status.FAIL)
                .translations(List.of(translation))
                .build();
    }

    static LanguageResult fail(List<Translation> translations) {
        return LanguageResultImpl.builder()
                .status(Result.Status.FAIL)
                .translations(translations)
                .build();
    }

    static LanguageResult fail(Translation translations, Throwable throwable) {
        return LanguageResultImpl.builder()
                .status(Result.Status.FAIL)
                .translations(List.of(translations))
                .throwable(throwable)
                .build();
    }


    static LanguageResult fail(List<Translation> translations, Throwable throwable) {
        return LanguageResultImpl.builder()
                .status(Result.Status.FAIL)
                .translations(translations)
                .throwable(throwable)
                .build();
    }

    static LanguageResult fail(Throwable throwable) {
        return LanguageResultImpl.builder()
                .status(Result.Status.FAIL)
                .throwable(throwable)
                .build();
    }

    static LanguageResult convert(Result result) {
        return LanguageResultImpl.builder()
                .status(result.getStatus())
                .build();
    }

    List<Template> getTemplates();

    default LanguageResult withTemplate(Template template) {
        getTemplates().add(template);
        return this;
    }

    default <T> DataResult<T> toData() {
        switch (this.getStatus()) {
            case FAIL:
                return DataResult.fail(getMessage());
            case OK:
                return DataResult.ok();
            default:
                return DataResult.unknown(getMessage());
        }
    }

    default SimpleResult toSimple() {
        switch (this.getStatus()) {
            case FAIL:
                return SimpleResult.fail(getMessage());
            case OK:
                return SimpleResult.ok();
            default:
                return SimpleResult.unknown(getMessage());
        }
    }
}
