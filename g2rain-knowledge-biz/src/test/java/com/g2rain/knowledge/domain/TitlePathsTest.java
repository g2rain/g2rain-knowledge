package com.g2rain.knowledge.domain;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TitlePathsTest {
    @Test
    void toJsonSerializesArray() {
        assertEquals("[\"Head\",\"Sub\"]", TitlePaths.toJson(List.of("Head", " Sub ", "")));
        assertNull(TitlePaths.toJson(List.of()));
        assertNull(TitlePaths.toJson(null));
    }

    @Test
    void toDisplayJoinsJsonArray() {
        assertEquals("Head > Sub", TitlePaths.toDisplay("[\"Head\",\"Sub\"]"));
        assertEquals("plain", TitlePaths.toDisplay("plain"));
        assertNull(TitlePaths.toDisplay(null));
        assertNull(TitlePaths.toDisplay("[]"));
    }
}
