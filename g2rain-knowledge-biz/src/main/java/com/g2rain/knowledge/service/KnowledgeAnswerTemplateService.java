package com.g2rain.knowledge.service;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.knowledge.dto.KnowledgeAnswerTemplateDto;
import com.g2rain.knowledge.dto.KnowledgeAnswerTemplateSelectDto;
import com.g2rain.knowledge.vo.KnowledgeAnswerTemplateVo;

import java.util.List;

/**
 * 知识回复模板表服务接口
 * 表名: knowledge_answer_template
 *
 * @author G2rain Generator
 */
public interface KnowledgeAnswerTemplateService {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return VO 对象列表
     */
    List<KnowledgeAnswerTemplateVo> selectList(KnowledgeAnswerTemplateSelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页 VO 数据
     */
    PageData<KnowledgeAnswerTemplateVo> selectPage(PageSelectListDto<KnowledgeAnswerTemplateSelectDto> selectDto);

    /**
     * 新增或更新数据
     *
     * @param dto 数据传输对象
     * @return 操作结果（影响行数）
     */
    Long save(KnowledgeAnswerTemplateDto dto);

    /**
     * 根据 ID 删除数据
     *
     * @param id 主键 ID
     * @return 操作结果（影响行数）
     */
    int delete(Long id);
}