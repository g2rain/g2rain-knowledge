package com.g2rain.knowledge.client.docling;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.client.storage.SourceMetadata;
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
import java.util.Objects;

/**
 * Adapter for Docling Serve's stable v1 multipart conversion endpoint. It asks
 * only for Markdown and plain text; chunk creation remains a controlled domain
 * concern rather than a provider-specific HTTP response.
 */
@Component
public class DoclingServeClient implements DoclingClient {
    private final DoclingProperties properties;

    public DoclingServeClient(DoclingProperties properties) {
        this.properties = properties;
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
            DoclingResponse response = client().post()
                .uri("/v1/convert/file")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(headers -> {
                    if (!isBlank(properties.getApiKey())) {
                        headers.set("X-Api-Key", properties.getApiKey());
                    }
                })
                .body(body)
                .retrieve()
                .body(DoclingResponse.class);
            return normalize(response);
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

    static ParsedDocument normalize(DoclingResponse response) {
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

    record DoclingResponse(String status, DoclingDocument document) {
    }

    record DoclingDocument(@JsonProperty("md_content") String markdown,
                           @JsonProperty("text_content") String text) {
    }
}
