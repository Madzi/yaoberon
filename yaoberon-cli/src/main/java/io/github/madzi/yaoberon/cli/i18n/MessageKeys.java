package io.github.madzi.yaoberon.cli.i18n;

import io.github.madzi.yaoberon.i18n.MessageService;

public enum MessageKeys implements MessageService.Key {
    CLI_WELCOME("cli.welcome"),
    CLI_VERSION("cli.version"),
    LSP_START("lsp.start"),
    FORMAT_START("format.start"),
    FORMAT_MISSING_INPUT("format.missingInput"),
    FORMAT_MISSING_FILTER("format.missingFilter"),
    FORMAT_SCAN_FAILED("format.scanFailed"),
    FORMAT_NO_MATCHES("format.noMatches"),
    FORMAT_FORMATTED_FILE("format.formattedFile"),
    FORMAT_FILE_FAILED("format.fileFailed"),
    FORMAT_INPUT_NOT_FOUND("format.inputNotFound"),
    FORMAT_INPUT_NOT_FILE("format.inputNotFile"),
    FORMAT_PARSE_FAILED("format.parseFailed"),
    FORMAT_SYNTAX_ERRORS("format.syntaxErrors"),
    FORMAT_FAILED("format.failed"),
    FORMAT_INVALID_INDENT_TYPE("format.invalidIndentType"),
    FORMAT_INVALID_INDENT_LENGTH_SPACES("format.invalidIndentLengthSpaces"),
    FORMAT_INVALID_INDENT_LENGTH_TABS("format.invalidIndentLengthTabs");

    private final String key;
    MessageKeys(String key) { this.key = key; }
    @Override public String key() { return key; }
}
