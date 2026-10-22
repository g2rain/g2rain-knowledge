package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeRevisionDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dto.KnowledgeCreateDto;
import com.g2rain.knowledge.dto.KnowledgeDraftCreateDto;
import com.g2rain.knowledge.dto.KnowledgeFaqEditDto;
import com.g2rain.knowledge.dto.KnowledgeImportOptionsDto;
import com.g2rain.knowledge.dto.KnowledgeReviewDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeDraftService;
import com.g2rain.knowledge.service.KnowledgeReviewService;
import com.g2rain.knowledge.service.KnowledgeSaveSupport;
import com.g2rain.knowledge.vo.KnowledgeImportItemVo;
import com.g2rain.knowledge.vo.KnowledgeImportResultVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeItemImportTest {

    @Mock
    private KnowledgeItemDao knowledgeItemDao;
    @Mock
    private KnowledgeRevisionDao knowledgeRevisionDao;
    @Mock
    private KnowledgeAccessService knowledgeAccessService;
    @Mock
    private KnowledgeSaveSupport knowledgeSaveSupport;
    @Mock
    private KnowledgeDraftService knowledgeDraftService;
    @Mock
    private KnowledgeReviewService knowledgeReviewService;
    @InjectMocks
    private KnowledgeItemServiceImpl service;

    @Test
    void importFaqSkipsExistingKey() {
        when(knowledgeAccessService.requireOrganId()).thenReturn(1L);
        KnowledgeItemPo existing = new KnowledgeItemPo();
        existing.setId(9L);
        existing.setKnowledgeKey("hello_faq");
        when(knowledgeItemDao.selectList(any())).thenReturn(List.of(existing));

        String csv = """
            knowledge_key,knowledge_name,faq_question,faq_answer
            hello_faq,Hello,Q,A
            """;
        MockMultipartFile file = new MockMultipartFile("file", "faq.csv", "text/csv",
            csv.getBytes(StandardCharsets.UTF_8));

        KnowledgeImportResultVo result = service.importFaq(new KnowledgeImportOptionsDto(), file);

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getSkippedCount());
        assertEquals(0, result.getFailedCount());
        assertEquals("SKIPPED", result.getItems().getFirst().getItemStatus());
        verify(knowledgeDraftService, never()).createKnowledge(any());
    }

    @Test
    void importFaqFailsInvalidKey() {
        String csv = """
            knowledge_key,knowledge_name,faq_question,faq_answer
            Bad-Key,Name,Q,A
            """;
        MockMultipartFile file = new MockMultipartFile("file", "faq.csv", "text/csv",
            csv.getBytes(StandardCharsets.UTF_8));

        KnowledgeImportResultVo result = service.importFaq(new KnowledgeImportOptionsDto(), file);

        assertEquals(1, result.getFailedCount());
        KnowledgeImportItemVo item = result.getItems().getFirst();
        assertEquals("FAILED", item.getItemStatus());
        assertEquals(KnowledgeErrorCode.IMPORT_FILE_INVALID.code(), item.getErrorCode());
    }

    @Test
    void importFaqSubmitsReviewWhenRequested() {
        when(knowledgeAccessService.requireOrganId()).thenReturn(1L);
        when(knowledgeItemDao.selectList(any())).thenReturn(List.of());
        when(knowledgeDraftService.createKnowledge(any(KnowledgeCreateDto.class))).thenReturn(11L);
        when(knowledgeDraftService.createDraft(any(KnowledgeDraftCreateDto.class))).thenReturn(22L);
        KnowledgeRevisionPo created = new KnowledgeRevisionPo();
        created.setId(22L);
        created.setVersion(0);
        KnowledgeRevisionPo afterFaq = new KnowledgeRevisionPo();
        afterFaq.setId(22L);
        afterFaq.setVersion(1);
        when(knowledgeRevisionDao.selectById(22L)).thenReturn(created, afterFaq);

        String csv = """
            knowledge_key,knowledge_name,faq_question,faq_answer
            hello_faq,Hello,Q,A
            """;
        MockMultipartFile file = new MockMultipartFile("file", "faq.csv", "text/csv",
            csv.getBytes(StandardCharsets.UTF_8));
        KnowledgeImportOptionsDto options = new KnowledgeImportOptionsDto();
        options.setSubmitReview(true);

        KnowledgeImportResultVo result = service.importFaq(options, file);

        assertEquals(1, result.getSuccessCount());
        verify(knowledgeDraftService).editFaq(any(KnowledgeFaqEditDto.class));
        ArgumentCaptor<KnowledgeReviewDto> reviewCaptor = ArgumentCaptor.forClass(KnowledgeReviewDto.class);
        verify(knowledgeReviewService).submit(reviewCaptor.capture());
        assertEquals(22L, reviewCaptor.getValue().getRevisionId());
        assertEquals(1, reviewCaptor.getValue().getExpectedVersion());
    }

    @Test
    void importFilesUsesFilenameStemAsKey() throws Exception {
        when(knowledgeAccessService.requireOrganId()).thenReturn(1L);
        when(knowledgeItemDao.selectList(any())).thenReturn(List.of());
        when(knowledgeDraftService.createKnowledge(any(KnowledgeCreateDto.class))).thenReturn(11L);
        when(knowledgeDraftService.createDraft(any(KnowledgeDraftCreateDto.class))).thenReturn(22L);
        KnowledgeRevisionPo revision = new KnowledgeRevisionPo();
        revision.setId(22L);
        revision.setVersion(0);
        when(knowledgeRevisionDao.selectById(22L)).thenReturn(revision);

        MockMultipartFile file = new MockMultipartFile("files", "policy_doc.md", "text/markdown",
            "# hello".getBytes(StandardCharsets.UTF_8));

        KnowledgeImportResultVo result = service.importFiles(new KnowledgeImportOptionsDto(), List.of(file));

        assertEquals(1, result.getSuccessCount());
        assertEquals("policy_doc", result.getItems().getFirst().getKnowledgeKey());
        ArgumentCaptor<KnowledgeCreateDto> createCaptor = ArgumentCaptor.forClass(KnowledgeCreateDto.class);
        verify(knowledgeDraftService).createKnowledge(createCaptor.capture());
        assertEquals("policy_doc", createCaptor.getValue().getKnowledgeKey());
        assertEquals("DOCUMENT", createCaptor.getValue().getKnowledgeType());
        verify(knowledgeDraftService).replaceFile(eq(22L), eq(0), any(), any());
        verify(knowledgeReviewService, never()).submit(any());
    }

    @Test
    void importFilesRejectsEmptyBatch() {
        BusinessException exception = assertThrows(BusinessException.class,
            () -> service.importFiles(new KnowledgeImportOptionsDto(), List.of()));
        assertEquals(KnowledgeErrorCode.IMPORT_FILE_INVALID.code(), exception.getErrorCode());
    }
}
