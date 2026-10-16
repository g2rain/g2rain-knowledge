package com.g2rain.knowledge.service.impl;

import com.g2rain.common.exception.BusinessException;
import com.g2rain.knowledge.dao.KnowledgeTagDao;
import com.g2rain.knowledge.dao.po.KnowledgeTagPo;
import com.g2rain.knowledge.domain.KnowledgeTagPolicy;
import com.g2rain.knowledge.dto.KnowledgeTagSelectDto;
import com.g2rain.knowledge.enums.KnowledgeEnablementStatus;
import com.g2rain.knowledge.enums.KnowledgeErrorCode;
import com.g2rain.knowledge.service.KnowledgeAccessService;
import com.g2rain.knowledge.service.KnowledgeTagGovernanceService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Keeps historical snapshots immutable while preventing disabled tags from
 * being selected on new or editable drafts.
 */
@Service
public class KnowledgeTagGovernanceServiceImpl implements KnowledgeTagGovernanceService {

    @Resource
    private KnowledgeTagDao knowledgeTagDao;
    @Resource
    private KnowledgeTagPolicy knowledgeTagPolicy;
    @Resource
    private KnowledgeAccessService knowledgeAccessService;

    @Override
    public List<String> validateDraftTags(List<String> codes) {
        List<String> normalized = knowledgeTagPolicy.normalize(codes);
        Long organId = knowledgeAccessService.requireOrganId();
        for (String code : normalized) {
            KnowledgeTagSelectDto query = new KnowledgeTagSelectDto();
            query.setOrganId(organId);
            query.setTagCode(code);
            query.setStatus(KnowledgeEnablementStatus.ACTIVE.name());
            List<KnowledgeTagPo> matches = knowledgeTagDao.selectList(query);
            if (matches.size() != 1) {
                throw new BusinessException(KnowledgeErrorCode.TAG_NOT_AVAILABLE);
            }
        }
        return normalized;
    }
}
