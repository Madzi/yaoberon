package io.github.madzi.yaoberon.i18n;

import org.junit.jupiter.api.Test;
import java.util.ResourceBundle;
import static org.junit.jupiter.api.Assertions.*;

class MessageServiceTest {

    private final MessageService service = MessageService.create(ResourceBundle.getBundle("test_bundle"));

    @Test
    void testTextWithKey() {
        MessageService.Key key = () -> "hello";
 assertEquals("Hello, %s!", service.text(key));
    }

    @Test
    void testTextWithArgs() {
        MessageService.Key key = () -> "hello";
        assertEquals("Hello, World!", service.text(key, "World"));
    }

    @Test
    void testTextNoArgs() {
        MessageService.Key key = () -> "welcome";
        assertEquals("Welcome to Oberon.", service.text(key));
    }
}
