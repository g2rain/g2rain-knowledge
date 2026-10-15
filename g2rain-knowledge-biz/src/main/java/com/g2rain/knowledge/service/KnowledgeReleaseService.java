package com.g2rain.knowledge.service;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.knowledge.dto.KnowledgeReleaseDto;
import com.g2rain.knowledge.dto.KnowledgeReleasePublishDto;
import com.g2rain.knowledge.dto.KnowledgeReleaseRollbackDto;
import com.g2rain.knowledge.dto.KnowledgeReleaseSelectDto;
import com.g2rain.knowledge.vo.KnowledgeReleaseVo;

import java.util.List;

/**
 * 知识发布选择表服务接口
 * 表名: knowledge_release
 *
 * @author G2rain Generator
 */
public interface KnowledgeReleaseService {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return VO 对象列表
     */
    List<KnowledgeReleaseVo> selectList(KnowledgeReleaseSelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页 VO 数据
     */
    PageData<KnowledgeReleaseVo> selectPage(PageSelectListDto<KnowledgeReleaseSelectDto> selectDto);

    /**
     * 新增或更新数据
     *
     * @param dto 数据传输对象
     * @return 操作结果（影响行数）
     */
    Long save(KnowledgeReleaseDto dto);

    /**
     * Publishes one ready revision as the active revision for its knowledge topic.
     *
     * @return the release record identifier
     */
    Long publish(KnowledgeReleasePublishDto dto);

    /**
     * Restores a deprecated, fully indexed revision as the active revision.
     *
     * @return the release record identifier
     */
    Long rollback(KnowledgeReleaseRollbackDto dto);

    /**
     * 根据 ID 删除数据
     *
     * @param id 主键 ID
     * @return 操作结果（影响行数）
     */
    int delete(Long id);
}
