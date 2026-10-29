package com.g2rain.knowledge.service.impl;

import com.g2rain.knowledge.client.milvus.MilvusKnowledgeClient;
import com.g2rain.knowledge.client.milvus.MilvusProperties;
import com.g2rain.knowledge.dao.KnowledgeItemDao;
import com.g2rain.knowledge.dao.KnowledgeReleaseDao;
import com.g2rain.knowledge.dao.po.KnowledgeItemPo;
import com.g2rain.knowledge.dao.po.KnowledgeReleasePo;
import com.g2rain.knowledge.dto.KnowledgeReleaseSelectDto;
import com.g2rain.knowledge.enums.KnowledgeVisibility;
import com.g2rain.knowledge.service.KnowledgeVisibilitySyncService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class KnowledgeVisibilitySyncServiceImpl implements KnowledgeVisibilitySyncService {
    @Resource
    private KnowledgeItemDao knowledgeItemDao;
    @Resource
    private KnowledgeReleaseDao knowledgeReleaseDao;
    @Resource
    private MilvusKnowledgeClient milvusKnowledgeClient;
    @Resource
    private MilvusProperties milvusProperties;

    @Override
    public void syncActiveRevision(Long knowledgeId) {
        KnowledgeItemPo item = knowledgeItemDao.selectById(knowledgeId);
        if (item == null) {
            log.warn("visibility sync skipped: item missing knowledgeId={}", knowledgeId);
            return;
        }
        String visibility = KnowledgeVisibility.parse(item.getVisibility()).name();
        KnowledgeReleaseSelectDto query = new KnowledgeReleaseSelectDto();
        query.setOrganId(item.getOrganId());
        query.setKnowledgeId(knowledgeId);
        List<KnowledgeReleasePo> releases = knowledgeReleaseDao.selectList(query);
        if (releases.isEmpty() || Objects.isNull(releases.getFirst().getActiveRevisionId())) {
            log.info("visibility sync skipped: no active revision knowledgeId={} visibility={}", knowledgeId, visibility);
            return;
        }
        Long revisionId = releases.getFirst().getActiveRevisionId();
        milvusKnowledgeClient.setVisibility(
            milvusProperties.getCollectionName(), item.getOrganId(), revisionId, visibility);
    }
}
