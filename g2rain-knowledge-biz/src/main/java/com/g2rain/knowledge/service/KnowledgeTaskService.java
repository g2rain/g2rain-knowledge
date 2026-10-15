package com.g2rain.knowledge.service;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.knowledge.dto.KnowledgeTaskDto;
import com.g2rain.knowledge.dto.KnowledgeTaskSelectDto;
import com.g2rain.knowledge.vo.KnowledgeTaskVo;

import java.util.List;

/**
 * 知识解析与索引处理任务表服务接口
 * 表名: knowledge_task
 *
 * @author G2rain Generator
 */
public interface KnowledgeTaskService {
    List<KnowledgeTaskVo> selectList(KnowledgeTaskSelectDto selectDto);

    PageData<KnowledgeTaskVo> selectPage(PageSelectListDto<KnowledgeTaskSelectDto> selectDto);

    Long save(KnowledgeTaskDto dto);

    int delete(Long id);

    /**
     * Starts the asynchronous processing chain for a revision.
     *
     * @param revisionId revision identifier
     * @return the PARSE task identifier
     */
    Long processRevision(Long revisionId);
}
