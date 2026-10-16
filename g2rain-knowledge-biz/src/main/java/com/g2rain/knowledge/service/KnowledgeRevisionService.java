package com.g2rain.knowledge.service;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.knowledge.dto.KnowledgeRevisionDto;
import com.g2rain.knowledge.dto.KnowledgeRevisionSelectDto;
import com.g2rain.knowledge.service.source.KnowledgeSourceDownload;
import com.g2rain.knowledge.vo.KnowledgeRevisionVo;

import java.util.List;

/**
 * 知识版本表服务接口
 * 表名: knowledge_revision
 *
 * @author G2rain Generator
 */
public interface KnowledgeRevisionService {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return VO 对象列表
     */
    List<KnowledgeRevisionVo> selectList(KnowledgeRevisionSelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页 VO 数据
     */
    PageData<KnowledgeRevisionVo> selectPage(PageSelectListDto<KnowledgeRevisionSelectDto> selectDto);

    /**
     * 新增或更新数据
     *
     * @param dto 数据传输对象
     * @return 操作结果（影响行数）
     */
    Long save(KnowledgeRevisionDto dto);

    /**
     * Opens a FILE revision source for authenticated download. Caller must close the stream.
     */
    KnowledgeSourceDownload downloadSource(Long revisionId);

    /**
     * 根据 ID 删除数据
     *
     * @param id 主键 ID
     * @return 操作结果（影响行数）
     */
    int delete(Long id);
}