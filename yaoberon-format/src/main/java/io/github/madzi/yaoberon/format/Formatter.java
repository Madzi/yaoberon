package io.github.madzi.yaoberon.format;

import io.github.madzi.yaoberon.format.internal.BaseFormatter;

public interface Formatter {

    FormatResult format(String text);

    static Formatter create(final String indentStep) {
        return new BaseFormatter(indentStep);
    }
}
