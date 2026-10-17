package com.g2rain.knowledge.client.storage;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Supported FILE source kinds for upload and PROCESS. PDF goes through Docling;
 * Markdown and HTML are read as UTF-8 text and sliced in-process.
 */
public enum SourceFileKind {
    PDF(".pdf", "application/pdf", true),
    MARKDOWN(".md", "text/markdown", false),
    HTML(".html", "text/html", false);

    private static final Set<String> MARKDOWN_TYPES = Set.of(
        "text/markdown", "text/x-markdown", "text/plain");
    private static final Set<String> HTML_TYPES = Set.of(
        "text/html", "application/xhtml+xml");

    private final String extension;
    private final String storedContentType;
    private final boolean requiresDocling;

    SourceFileKind(String extension, String storedContentType, boolean requiresDocling) {
        this.extension = extension;
        this.storedContentType = storedContentType;
        this.requiresDocling = requiresDocling;
    }

    public String extension() {
        return extension;
    }

    public String storedContentType() {
        return storedContentType;
    }

    public boolean requiresDocling() {
        return requiresDocling;
    }

    public static SourceFileKind resolve(String fileName, String contentType) {
        String name = Objects.requireNonNullElse(fileName, "").toLowerCase(Locale.ROOT);
        String type = Objects.requireNonNullElse(contentType, "").trim().toLowerCase(Locale.ROOT);
        int semicolon = type.indexOf(';');
        if (semicolon >= 0) {
            type = type.substring(0, semicolon).trim();
        }

        if (name.endsWith(".pdf")) {
            return "application/pdf".equals(type) ? PDF : null;
        }
        if (name.endsWith(".md") || name.endsWith(".markdown")) {
            return MARKDOWN_TYPES.contains(type) || type.isEmpty() ? MARKDOWN : null;
        }
        if (name.endsWith(".html") || name.endsWith(".htm")) {
            return HTML_TYPES.contains(type) || type.isEmpty() ? HTML : null;
        }
        return null;
    }

    public static SourceFileKind fromStored(String fileName, String contentType) {
        SourceFileKind resolved = resolve(fileName, contentType);
        if (resolved != null) {
            return resolved;
        }
        String name = Objects.requireNonNullElse(fileName, "").toLowerCase(Locale.ROOT);
        String type = Objects.requireNonNullElse(contentType, "").trim().toLowerCase(Locale.ROOT);
        if (name.endsWith(".pdf") || "application/pdf".equals(type)) {
            return PDF;
        }
        if (name.endsWith(".html") || name.endsWith(".htm") || type.startsWith("text/html")
            || "application/xhtml+xml".equals(type)) {
            return HTML;
        }
        if (name.endsWith(".md") || name.endsWith(".markdown") || type.startsWith("text/markdown")
            || "text/x-markdown".equals(type) || "text/plain".equals(type)) {
            return MARKDOWN;
        }
        return null;
    }
}
