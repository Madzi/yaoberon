package io.github.madzi.yaoberon.i18n;

import java.util.ResourceBundle;

import io.github.madzi.yaoberon.i18n.internal.BaseMessageService;

public interface MessageService {
    String text(Key key);
    String text(Key key, Object... args);
    interface Key {
        String key();
    }
    static MessageService create(final ResourceBundle bundle) {
        return new BaseMessageService(bundle);
    }
}
