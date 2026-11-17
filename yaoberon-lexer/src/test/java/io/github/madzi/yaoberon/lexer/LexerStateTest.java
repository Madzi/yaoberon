package io.github.madzi.yaoberon.lexer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LexerStateTest {

    @Test
    void testInitialState() {
        assertEquals(0, LexerState.INITIAL.commentNestingDepth());
    }

    @Test
    void testCustomState() {
        LexerState state = new LexerState(3);
        assertEquals(3, state.commentNestingDepth());
        assertEquals(new LexerState(3), state);
        assertEquals(new LexerState(3).hashCode(), state.hashCode());
        assertNotEquals(LexerState.INITIAL, state);
        assertNotNull(state.toString());
    }
}
