package io.github.madzi.yaoberon.cli.i18n;

import io.github.madzi.yaoberon.i18n.MessageService;

public enum MessageKeys implements MessageService.Key {
    CLI_WELCOME("cli.welcome"),
    CLI_VERSION("cli.version"),
    LSP_START("lsp.start"),
    FORMAT_START("format.start");

    private final String key;
    MessageKeys(String key) { this.key = key; }
    @Override public String key() { return key; }
}
