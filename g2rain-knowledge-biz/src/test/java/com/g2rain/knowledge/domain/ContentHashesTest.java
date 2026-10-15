package com.g2rain.knowledge.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentHashesTest {

    @Test
    void acceptsMatchingUtf8Sha256() {
        String body = "证据正文";
        assertTrue(ContentHashes.matches(body, ContentHashes.sha256Utf8(body)));
    }

    @Test
    void discardsMismatchedOrMissingHash() {
        assertFalse(ContentHashes.matches("a", ContentHashes.sha256Utf8("b")));
        assertFalse(ContentHashes.matches("a", null));
        assertFalse(ContentHashes.matches("a", " "));
        assertFalse(ContentHashes.matches(null, ContentHashes.sha256Utf8("a")));
    }
}
