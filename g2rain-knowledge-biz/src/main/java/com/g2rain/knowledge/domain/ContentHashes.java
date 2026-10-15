package com.g2rain.knowledge.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** SHA-256 helpers for chunk body verification. */
public final class ContentHashes {
    private ContentHashes() {
    }

    public static String sha256Utf8(String content) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
    }

    public static boolean matches(String content, String expectedSha256) {
        if (expectedSha256 == null || expectedSha256.isBlank() || content == null) {
            return false;
        }
        return expectedSha256.equalsIgnoreCase(sha256Utf8(content));
    }
}
