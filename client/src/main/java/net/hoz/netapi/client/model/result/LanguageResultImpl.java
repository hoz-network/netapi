package net.hoz.netapi.client.model.result;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import net.hoz.api.commons.Result;
import net.kyori.adventure.text.minimessage.Template;
import org.screamingsandals.lib.lang.Translation;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Builder(access = AccessLevel.PACKAGE)
@Getter
@ToString
class LanguageResultImpl implements LanguageResult {
    private final List<Template> templates = new LinkedList<>();
    private final Result.Status status;
    private final List<Translation> translations;
    private final Throwable throwable;

    @Override
    public String getMessage() {
        return translations
                .stream()
                .map(next -> String.join(".", next.getKeys()))
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean isOk() {
        return status == Result.Status.OK;
    }

    @Override
    public boolean isFail() {
        return status == Result.Status.FAIL;
    }
}
