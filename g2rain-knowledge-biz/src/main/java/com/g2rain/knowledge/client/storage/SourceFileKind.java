package com.g2rain.knowledge.client.storage;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;

/**
 * Supported FILE source kinds for upload and PROCESS. All FILE kinds go through
 * Docling Serve HybridChunker ({@code requiresDocling=true}).
 * <p>
 * Plain {@code .txt} uses Docling {@code md} (Docling maps txt/text to the Markdown backend).
 * Legacy {@code .doc}/{@code .ppt}/{@code .xls} may require LibreOffice on the Serve host;
 * Serve v1.29 OpenAPI may omit those enums, in which case {@code convert_from_formats} is skipped.
 */
public enum SourceFileKind {
    PDF(".pdf", "application/pdf", "pdf", true),
    DOCX(".docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document", "docx", true),
    DOC(".doc", "application/msword", "doc", true),
    PPTX(".pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation", "pptx", true),
    PPT(".ppt", "application/vnd.ms-powerpoint", "ppt", true),
    XLSX(".xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", true),
    XLS(".xls", "application/vnd.ms-excel", "xls", true),
    MARKDOWN(".md", "text/markdown", "md", false),
    TEXT(".txt", "text/plain", "md", false),
    HTML(".html", "text/html", "html", false);

    private static final Set<String> MARKDOWN_TYPES = Set.of(
        "text/markdown", "text/x-markdown");
    private static final Set<String> TEXT_TYPES = Set.of(
        "text/plain", "text/txt");
    private static final Set<String> HTML_TYPES = Set.of(
        "text/html", "application/xhtml+xml");
    private static final Set<String> DOCX_TYPES = Set.of(
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
        "application/octet-stream");
    private static final Set<String> DOC_TYPES = Set.of(
        "application/msword",
        "application/x-msword",
        "application/octet-stream");
    private static final Set<String> PPTX_TYPES = Set.of(
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",
        "application/vnd.openxmlformats-officedocument.presentationml.slideshow",
        "application/octet-stream");
    private static final Set<String> PPT_TYPES = Set.of(
        "application/vnd.ms-powerpoint",
        "application/mspowerpoint",
        "application/octet-stream");
    private static final Set<String> XLSX_TYPES = Set.of(
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
        "application/octet-stream");
    private static final Set<String> XLS_TYPES = Set.of(
        "application/vnd.ms-excel",
        "application/msexcel",
        "application/octet-stream");

    private final String extension;
    private final String storedContentType;
    private final String doclingFromFormat;
    private final boolean binary;

    SourceFileKind(String extension, String storedContentType, String doclingFromFormat, boolean binary) {
        this.extension = extension;
        this.storedContentType = storedContentType;
        this.doclingFromFormat = doclingFromFormat;
        this.binary = binary;
    }

    public String extension() {
        return extension;
    }

    public String storedContentType() {
        return storedContentType;
    }

    public boolean requiresDocling() {
        return true;
    }

    /** True for PDF / Office binaries (not UTF-8 text sniffing). */
    public boolean binary() {
        return binary;
    }

    /** Whether OOXML/ZIP magic ({@code PK..}) is expected. */
    public boolean zipContainer() {
        return this == DOCX || this == PPTX || this == XLSX;
    }

    /** Whether OLE compound-file magic is expected. */
    public boolean oleContainer() {
        return this == DOC || this == PPT || this == XLS;
    }

    /** Docling Serve {@code convert_from_formats} / InputFormat value. */
    public String doclingFromFormat() {
        return doclingFromFormat;
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
        if (name.endsWith(".docx")) {
            return DOCX_TYPES.contains(type) || type.isEmpty() ? DOCX : null;
        }
        if (name.endsWith(".doc")) {
            return DOC_TYPES.contains(type) || type.isEmpty() ? DOC : null;
        }
        if (name.endsWith(".pptx")) {
            return PPTX_TYPES.contains(type) || type.isEmpty() ? PPTX : null;
        }
        if (name.endsWith(".ppt")) {
            return PPT_TYPES.contains(type) || type.isEmpty() ? PPT : null;
        }
        if (name.endsWith(".xlsx")) {
            return XLSX_TYPES.contains(type) || type.isEmpty() ? XLSX : null;
        }
        if (name.endsWith(".xls")) {
            return XLS_TYPES.contains(type) || type.isEmpty() ? XLS : null;
        }
        if (name.endsWith(".txt") || name.endsWith(".text")) {
            return TEXT_TYPES.contains(type) || type.isEmpty() ? TEXT : null;
        }
        if (name.endsWith(".md") || name.endsWith(".markdown")) {
            return MARKDOWN_TYPES.contains(type) || "text/plain".equals(type) || type.isEmpty() ? MARKDOWN : null;
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
        if (name.endsWith(".docx") || type.contains("wordprocessingml")) {
            return DOCX;
        }
        if (name.endsWith(".doc") || "application/msword".equals(type) || "application/x-msword".equals(type)) {
            return DOC;
        }
        if (name.endsWith(".pptx") || type.contains("presentationml")) {
            return PPTX;
        }
        if (name.endsWith(".ppt") || type.contains("ms-powerpoint") || "application/mspowerpoint".equals(type)) {
            return PPT;
        }
        if (name.endsWith(".xlsx") || type.contains("spreadsheetml")) {
            return XLSX;
        }
        if (name.endsWith(".xls") || type.contains("ms-excel") || "application/msexcel".equals(type)) {
            return XLS;
        }
        if (name.endsWith(".html") || name.endsWith(".htm") || type.startsWith("text/html")
            || "application/xhtml+xml".equals(type)) {
            return HTML;
        }
        if (name.endsWith(".txt") || name.endsWith(".text")) {
            return TEXT;
        }
        if (name.endsWith(".md") || name.endsWith(".markdown") || type.startsWith("text/markdown")
            || "text/x-markdown".equals(type) || "text/plain".equals(type)) {
            return MARKDOWN;
        }
        return null;
    }
}
