package com.g2rain.knowledge.service;

import com.g2rain.knowledge.dto.KnowledgeRetrieveDto;
import com.g2rain.knowledge.vo.KnowledgeRetrieveVo;

public interface KnowledgeRetrieveService {
    KnowledgeRetrieveVo retrieve(KnowledgeRetrieveDto dto);
}
