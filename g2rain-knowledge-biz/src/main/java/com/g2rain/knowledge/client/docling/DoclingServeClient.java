package com.g2rain.knowledge.client.docling;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.client.storage.SourceMetadata;
import com.g2rain.knowledge.domain.ChunkContent;
import com.g2rain.knowledge.domain.ChunkingProfile;
import com.g2rain.knowledge.domain.ChunkingResult;
import com.g2rain.knowledge.domain.KnowledgeChunkPolicy;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Adapter for Docling Serve v1 convert and HybridChunker endpoints.
 */
@Component
public class DoclingServeClient implements DoclingClient {
    /**
     * Serve v1.29 OpenAPI {@code InputFormat} values accepted on multipart
     * {@code convert_from_formats}. Formats outside this set (e.g. legacy {@code doc})
     * omit the field so Docling can sniff by filename when the host supports them.
     */
    private static final Set<String> SERVE_DECLARED_FROM_FORMATS = Set.of(
        "docx", "pptx", "html", "image", "pdf", "asciidoc", "md", "csv", "xlsx",
        "xml_uspto", "xml_jats", "mets_gbs", "json_docling", "audio");

    private final DoclingProperties properties;
    private final KnowledgeChunkPolicy knowledgeChunkPolicy;

    public DoclingServeClient(DoclingProperties properties, KnowledgeChunkPolicy knowledgeChunkPolicy) {
        this.properties = properties;
        this.knowledgeChunkPolicy = knowledgeChunkPolicy;
    }

    @Override
    public ParsedDocument parse(InputStream content, SourceMetadata metadata) {
        if (Objects.isNull(content) || Objects.isNull(metadata) || isBlank(properties.getBaseUrl())) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_NOT_CONFIGURED);
        }
        try {
            byte[] bytes = content.readAllBytes();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("files", new NamedByteArrayResource(bytes, metadata.originalFileName()));
            body.add("from_formats", "pdf");
            body.add("to_formats", "md");
            body.add("to_formats", "text");
            DoclingConvertResponse response = client().post()
                .uri("/v1/convert/file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(headers -> {
                    if (!isBlank(properties.getApiKey())) {
                        headers.set("X-Api-Key", properties.getApiKey());
                    }
                })
                .body(body)
                .retrieve()
                .body(DoclingConvertResponse.class);
            return normalizeConvert(response);
        } catch (IOException | RestClientException exception) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_OPERATION_FAILED);
        }
    }

    @Override
    public ChunkingResult chunkFile(InputStream content, SourceMetadata metadata, String fromFormat,
                                    ChunkingProfile profile) {
        if (Objects.isNull(content) || Objects.isNull(metadata) || isBlank(properties.getBaseUrl())) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_NOT_CONFIGURED);
        }
        if (isBlank(fromFormat)) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_INVALID_RESPONSE);
        }
        ChunkingProfile effective = profile == null
            ? ChunkingProfile.defaults(properties.getChunkingMaxTokens())
            : profile;
        try {
            byte[] bytes = content.readAllBytes();
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("files", new NamedByteArrayResource(bytes, metadata.originalFileName()));
            body.add("include_converted_doc", "true");
            if (SERVE_DECLARED_FROM_FORMATS.contains(fromFormat)) {
                body.add("convert_from_formats", fromFormat);
            }
            body.add("chunking_include_raw_text", "true");
            body.add("chunking_merge_peers", Boolean.toString(effective.mergePeers()));
            body.add("chunking_use_markdown_tables", "true");
            body.add("chunking_max_tokens", Integer.toString(effective.maxTokens()));
            if (!isBlank(effective.tokenizer())) {
                body.add("chunking_tokenizer", effective.tokenizer());
            }
            ChunkDocumentResponse response = client().post()
                .uri("/v1/chunk/hybrid/file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(headers -> {
                    if (!isBlank(properties.getApiKey())) {
                        headers.set("X-Api-Key", properties.getApiKey());
                    }
                })
                .body(body)
                .retrieve()
                .body(ChunkDocumentResponse.class);
            return normalizeChunk(response);
        } catch (IOException | RestClientException exception) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_OPERATION_FAILED);
        }
    }

    private RestClient client() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder().baseUrl(properties.getBaseUrl()).requestFactory(factory).build();
    }

    static ParsedDocument normalizeConvert(DoclingConvertResponse response) {
        if (Objects.isNull(response) || !("success".equals(response.status()) || "partial_success".equals(response.status()))
            || Objects.isNull(response.document())) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_INVALID_RESPONSE);
        }
        String markdown = nullToEmpty(response.document().markdown());
        String text = nullToEmpty(response.document().text());
        if (markdown.isBlank() && text.isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_INVALID_RESPONSE);
        }
        return new ParsedDocument(markdown, text);
    }

    /** Package-visible for unit tests. */
    ChunkingResult normalizeChunk(ChunkDocumentResponse response) {
        if (Objects.isNull(response) || response.chunks() == null || response.chunks().isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_INVALID_RESPONSE);
        }
        List<ChunkedDocumentResultItem> ordered = response.chunks().stream()
            .filter(Objects::nonNull)
            .sorted(Comparator.comparingInt(ChunkedDocumentResultItem::chunkIndex))
            .toList();
        if (ordered.isEmpty()) {
            throw new BusinessException(KnowledgeErrorCode.DOCLING_INVALID_RESPONSE);
        }
        List<ChunkContent> chunks = new ArrayList<>(ordered.size());
        for (int i = 0; i < ordered.size(); i++) {
            ChunkedDocumentResultItem item = ordered.get(i);
            int no = i + 1;
            String embeddingText = nullToEmpty(item.text());
            String rawText = item.rawText() == null || item.rawText().isBlank()
                ? embeddingText
                : item.rawText();
            if (rawText.isBlank()) {
                throw new BusinessException(KnowledgeErrorCode.DOCLING_INVALID_RESPONSE);
            }
            String titlePath = joinHeadings(item.headings());
            Integer pageStart = null;
            Integer pageEnd = null;
            if (item.pageNumbers() != null && !item.pageNumbers().isEmpty()) {
                pageStart = item.pageNumbers().stream().filter(Objects::nonNull).min(Integer::compareTo).orElse(null);
                pageEnd = item.pageNumbers().stream().filter(Objects::nonNull).max(Integer::compareTo).orElse(null);
            }
            int tokens = item.numTokens() != null && item.numTokens() > 0
                ? item.numTokens()
                : knowledgeChunkPolicy.estimateTokens(embeddingText.isBlank() ? rawText : embeddingText);
            chunks.add(knowledgeChunkPolicy.build(
                no, "chunk:" + no, rawText, embeddingText.isBlank() ? rawText : embeddingText,
                tokens, pageStart, pageEnd, titlePath));
        }
        String markdown = extractMarkdown(response.documents());
        if (markdown.isBlank()) {
            StringBuilder builder = new StringBuilder();
            for (ChunkContent chunk : chunks) {
                if (builder.length() > 0) {
                    builder.append("\n\n");
                }
                builder.append(chunk.rawText());
            }
            markdown = builder.toString();
        }
        return new ChunkingResult(markdown, List.copyOf(chunks), KnowledgeChunkPolicy.PARSER_DOCLING_HYBRID);
    }

    private static String extractMarkdown(List<ExportResult> documents) {
        if (documents == null) {
            return "";
        }
        for (ExportResult document : documents) {
            if (document == null || document.content() == null) {
                continue;
            }
            String markdown = nullToEmpty(document.content().markdown());
            if (!markdown.isBlank()) {
                return markdown;
            }
            String text = nullToEmpty(document.content().text());
            if (!text.isBlank()) {
                return text;
            }
        }
        return "";
    }

    private static String joinHeadings(List<String> headings) {
        if (headings == null || headings.isEmpty()) {
            return null;
        }
        List<String> cleaned = headings.stream()
            .filter(h -> h != null && !h.isBlank())
            .map(String::trim)
            .toList();
        return cleaned.isEmpty() ? null : String.join(" > ", cleaned);
    }

    private static String nullToEmpty(String value) {
        return Objects.requireNonNullElse(value, "");
    }

    private static boolean isBlank(String value) {
        return Objects.isNull(value) || value.isBlank();
    }

    private static final class NamedByteArrayResource extends ByteArrayResource {
        private final String filename;

        private NamedByteArrayResource(byte[] byteArray, String filename) {
            super(byteArray);
            this.filename = filename;
        }

        @Override
        public String getFilename() {
            return filename;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DoclingConvertResponse(String status, DoclingDocument document) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DoclingDocument(@JsonProperty("md_content") String markdown,
                           @JsonProperty("text_content") String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChunkDocumentResponse(List<ChunkedDocumentResultItem> chunks,
                                 List<ExportResult> documents,
                                 @JsonProperty("processing_time") Double processingTime) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChunkedDocumentResultItem(
        String filename,
        @JsonProperty("chunk_index") int chunkIndex,
        String text,
        @JsonProperty("raw_text") String rawText,
        @JsonProperty("num_tokens") Integer numTokens,
        List<String> headings,
        List<String> captions,
        @JsonProperty("doc_items") List<String> docItems,
        @JsonProperty("page_numbers") List<Integer> pageNumbers
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExportResult(ExportDocumentContent content, String status) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ExportDocumentContent(
        @JsonProperty("md_content") String markdown,
        @JsonProperty("text_content") String text
    ) {
    }
}
