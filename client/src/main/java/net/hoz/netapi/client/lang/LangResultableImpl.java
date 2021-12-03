package net.hoz.netapi.client.lang;

import com.iamceph.resulter.core.api.ResultStatus;
import com.iamceph.resulter.core.model.Resulters;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.minimessage.Template;
import org.screamingsandals.lib.lang.Translation;

import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

@Builder(access = AccessLevel.PACKAGE)
@Accessors(fluent = true)
@Getter
@ToString
class LangResultableImpl implements LangResultable {
    private final List<Template> templates = new LinkedList<>();
    private final ResultStatus status;
    private final List<Translation> translations;
    private final Throwable error;

    @Override
    public String message() {
        return translations
                .stream()
                .map(next -> String.join(".", next.getKeys()))
                .collect(Collectors.joining(", "));
    }

    @Override
    public boolean isOk() {
        return status == ResultStatus.OK;
    }

    @Override
    public boolean isFail() {
        return status == ResultStatus.FAIL;
    }

    @Override
    public boolean isWarning() {
        return status == ResultStatus.WARNING;
    }

    /**
     * @see com.iamceph.resulter.core.Resultable#convertor()
     */
    @Override
    public Convertor convertor() {
        return new Convertor() {
            @Override
            public String json() {
                return Resulters.convertor().json(LangResultableImpl.this);
            }

            @Override
            public <K> K convert(Class<K> target) {
                return Resulters.convertor().convert(LangResultableImpl.this, target);
            }
        };
    }
}
