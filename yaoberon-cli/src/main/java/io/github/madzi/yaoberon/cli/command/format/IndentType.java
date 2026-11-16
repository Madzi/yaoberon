package io.github.madzi.yaoberon.cli.command.format;

import java.util.Locale;

public enum IndentType {
    TABS("\t"),
    SPACES(" ");

    private final String value;

    IndentType(final String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    public static IndentType from(final String value) {
        return null == value ? null : IndentType.valueOf(value.toUpperCase(Locale.ROOT));
    }
}
