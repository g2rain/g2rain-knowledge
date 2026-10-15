package com.g2rain.knowledge.service.impl;

import com.g2rain.knowledge.client.model.KnowledgeChatClient;
import com.g2rain.knowledge.dao.KnowledgeAnswerTemplateDao;
import com.g2rain.knowledge.dao.po.KnowledgeAnswerTemplatePo;
import com.g2rain.knowledge.dto.KnowledgeAnswerDto;
import com.g2rain.knowledge.dto.KnowledgeAnswerTemplateSelectDto;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeAnswerService;
import com.g2rain.knowledge.service.KnowledgeRetrieveService;
import com.g2rain.knowledge.vo.KnowledgeAnswerVo;
import com.g2rain.knowledge.vo.KnowledgeEvidenceVo;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class KnowledgeAnswerServiceImpl implements KnowledgeAnswerService {
    private static final String NO_EVIDENCE_ANSWER = "未找到能够确认该问题的已发布知识。";

    @Resource private KnowledgeRetrieveService knowledgeRetrieveService;
    @Resource private KnowledgeChatClient knowledgeChatClient;
    @Resource private KnowledgeAnswerTemplateDao knowledgeAnswerTemplateDao;
    @Resource private KnowledgeAccessService knowledgeAccessService;

    @Override
    public KnowledgeAnswerVo answer(KnowledgeAnswerDto dto) {
        KnowledgeRetrieveVo retrieved = knowledgeRetrieveService.retrieve(dto);
        KnowledgeAnswerVo result = new KnowledgeAnswerVo();
        result.setAnswerable(retrieved.isAnswerable());
        result.setTraceId(retrieved.getTraceId());
        result.setReasonCode(retrieved.getReasonCode());
        result.setSources(retrieved.getSources());
        if (!retrieved.isAnswerable()) {
            result.setAnswer(NO_EVIDENCE_ANSWER);
            return result;
        }
        KnowledgeAnswerTemplatePo template = template(dto.getTemplateCode());
        result.setAnswer(knowledgeChatClient.complete(template.getSystemPrompt(), prompt(dto.getQuery().trim(), retrieved)));
        return result;
    }

    private KnowledgeAnswerTemplatePo template(String templateCode) {
        KnowledgeAnswerTemplateSelectDto query = new KnowledgeAnswerTemplateSelectDto();
        query.setOrganId(knowledgeAccessService.requireOrganId());
        query.setTemplateCode(templateCode == null || templateCode.isBlank() ? "DEFAULT" : templateCode.trim());
        query.setStatus("ACTIVE");
        List<KnowledgeAnswerTemplatePo> templates = knowledgeAnswerTemplateDao.selectList(query);
        if (templates.size() != 1 || templates.getFirst().getSystemPrompt() == null || templates.getFirst().getSystemPrompt().isBlank()) {
            throw new BusinessException(KnowledgeErrorCode.ANSWER_TEMPLATE_NOT_AVAILABLE);
        }
        return templates.getFirst();
    }

    private String prompt(String question, KnowledgeRetrieveVo retrieved) {
        String evidence = java.util.stream.IntStream.range(0, retrieved.getSources().size())
            .mapToObj(index -> source(index + 1, retrieved.getSources().get(index)))
            .collect(Collectors.joining("\n\n"));
        return "问题：\n" + question + "\n\n知识证据：\n" + evidence;
    }

    private String source(int number, KnowledgeEvidenceVo source) {
        return "[" + number + "] " + source.getCitation() + "\n" + source.getContent();
    }
}
