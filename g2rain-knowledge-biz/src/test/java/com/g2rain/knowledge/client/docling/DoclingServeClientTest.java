package com.g2rain.knowledge.client.docling;

import com.g2rain.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DoclingServeClientTest {
    @Test
    void normalizesSuccessfulResponse() {
        ParsedDocument result = DoclingServeClient.normalize(
            new DoclingServeClient.DoclingResponse("success",
                new DoclingServeClient.DoclingDocument("# title", "title")));

        assertEquals("# title", result.markdown());
        assertEquals("title", result.text());
    }

    @Test
    void rejectsFailureOrEmptyContent() {
        assertThrows(BusinessException.class, () -> DoclingServeClient.normalize(
            new DoclingServeClient.DoclingResponse("failure", null)));
        assertThrows(BusinessException.class, () -> DoclingServeClient.normalize(
            new DoclingServeClient.DoclingResponse("success",
                new DoclingServeClient.DoclingDocument("", ""))));
    }
}
