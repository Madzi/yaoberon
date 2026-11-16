package io.github.madzi.yaoberon.cli.command.format;

import io.github.madzi.yaoberon.cli.Main;
import io.github.madzi.yaoberon.cli.i18n.MessageKeys;

public record FormatConfig(String indent) {

    public static Builder builder() {
        return new Builder();
    }
    
    public static class Builder {
        private IndentType type = IndentType.SPACES;
        private int count = 2;
        public Builder type(final String value) {
            try {
                return type(IndentType.from(value));
            } catch (final IllegalArgumentException exception) {
                throw new IllegalArgumentException(Main.I18N.text(MessageKeys.FORMAT_INVALID_INDENT_TYPE, value));
            }
        }
        public Builder type(final IndentType type) {
            if (null == type) {
                throw new IllegalArgumentException(Main.I18N.text(MessageKeys.FORMAT_INVALID_INDENT_TYPE, type));
            }
            this.type = type;
            return this;
        }
        public Builder count(final int count) {
            if ((count < 1) || (IndentType.SPACES == type && count > 8) || (IndentType.TABS == type && count > 2)) {
                throw new IllegalArgumentException(Main.I18N.text(IndentType.SPACES == type ? MessageKeys.FORMAT_INVALID_INDENT_LENGTH_SPACES : MessageKeys.FORMAT_INVALID_INDENT_LENGTH_TABS, count));                 
            }
            this.count = count;
            return this;
        }
        public FormatConfig build() {
            return new FormatConfig(type.value().repeat(count));
        }
    }
}
