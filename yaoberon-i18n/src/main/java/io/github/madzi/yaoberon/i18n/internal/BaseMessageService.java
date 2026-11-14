package io.github.madzi.yaoberon.i18n.internal;

import java.util.ResourceBundle;

import io.github.madzi.yaoberon.i18n.MessageService;

public class BaseMessageService implements MessageService {

    private final ResourceBundle bundle;

    public BaseMessageService(final ResourceBundle bundle) {
        this.bundle = bundle;
    }

    @Override
    public String text(final Key key) {
        return bundle.getString(key.key());
    }

    @Override
    public String text(final Key key, final Object... args) {
        return bundle.getString(key.key()).formatted(args);
    }
}
