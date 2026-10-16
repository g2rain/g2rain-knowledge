package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.client.KnowledgeDependencyException;
import com.g2rain.knowledge.client.model.KnowledgeChatClient;
import com.g2rain.knowledge.dao.KnowledgeAnswerTemplateDao;
import com.g2rain.knowledge.dao.po.KnowledgeAnswerTemplatePo;
import com.g2rain.knowledge.dto.KnowledgeAnswerDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeRetrieveService;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KnowledgeAnswerServiceImplTest {

    @Mock
    private KnowledgeRetrieveService knowledgeRetrieveService;
    @Mock
    private KnowledgeChatClient knowledgeChatClient;
    @Mock
    private KnowledgeAnswerTemplateDao knowledgeAnswerTemplateDao;
    @Mock
    private KnowledgeAccessService knowledgeAccessService;
    @InjectMocks
    private KnowledgeAnswerServiceImpl service;

    @Test
    void mapsChatFailureToDependencyException() {
        when(knowledgeRetrieveService.retrieve(any())).thenReturn(answerable());
        when(knowledgeAccessService.requireOrganId()).thenReturn(1L);
        when(knowledgeAnswerTemplateDao.selectList(any())).thenReturn(List.of(template()));
        when(knowledgeChatClient.complete(any(), any()))
            .thenThrow(new BusinessException(KnowledgeErrorCode.ANSWER_OPERATION_FAILED));

        KnowledgeDependencyException exception = assertThrows(
            KnowledgeDependencyException.class, () -> service.answer(request()));

        assertEquals("chat", exception.getDependency());
        assertEquals(KnowledgeErrorCode.ANSWER_OPERATION_FAILED.code(), exception.getErrorCode());
    }

    @Test
    void keepsMissingTemplateAsBusinessError() {
        when(knowledgeRetrieveService.retrieve(any())).thenReturn(answerable());
        when(knowledgeAccessService.requireOrganId()).thenReturn(1L);
        when(knowledgeAnswerTemplateDao.selectList(any())).thenReturn(List.of());

        BusinessException exception = assertThrows(BusinessException.class, () -> service.answer(request()));

        assertEquals(KnowledgeErrorCode.ANSWER_TEMPLATE_NOT_AVAILABLE.code(), exception.getErrorCode());
    }

    private static KnowledgeAnswerDto request() {
        KnowledgeAnswerDto dto = new KnowledgeAnswerDto();
        dto.setQuery("退款多久到账");
        return dto;
    }

    private static KnowledgeRetrieveVo answerable() {
        KnowledgeRetrieveVo retrieved = new KnowledgeRetrieveVo();
        retrieved.setAnswerable(true);
        retrieved.setSources(List.of());
        return retrieved;
    }

    private static KnowledgeAnswerTemplatePo template() {
        KnowledgeAnswerTemplatePo template = new KnowledgeAnswerTemplatePo();
        template.setSystemPrompt("只根据证据回答");
        return template;
    }
}
