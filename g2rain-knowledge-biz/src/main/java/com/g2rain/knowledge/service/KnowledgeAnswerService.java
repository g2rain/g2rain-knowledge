package com.g2rain.knowledge.service;

import com.g2rain.knowledge.dto.KnowledgeAnswerDto;
import com.g2rain.knowledge.vo.KnowledgeAnswerVo;

public interface KnowledgeAnswerService {
    KnowledgeAnswerVo answer(KnowledgeAnswerDto dto);
}
