package com.g2rain.knowledge.service;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.knowledge.dto.KnowledgeImportOptionsDto;
import com.g2rain.knowledge.dto.KnowledgeItemDto;
import com.g2rain.knowledge.dto.KnowledgeItemSelectDto;
import com.g2rain.knowledge.vo.KnowledgeImportResultVo;
import com.g2rain.knowledge.vo.KnowledgeItemVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识主题表服务接口
 * 表名: knowledge_item
 *
 * @author G2rain Generator
 */
public interface KnowledgeItemService {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return VO 对象列表
     */
    List<KnowledgeItemVo> selectList(KnowledgeItemSelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页 VO 数据
     */
    PageData<KnowledgeItemVo> selectPage(PageSelectListDto<KnowledgeItemSelectDto> selectDto);

    /**
     * 新增或更新数据
     *
     * @param dto 数据传输对象
     * @return 操作结果（影响行数）
     */
    Long save(KnowledgeItemDto dto);

    /**
     * 根据 ID 删除数据
     *
     * @param id 主键 ID
     * @return 操作结果（影响行数）
     */
    int delete(Long id);

    /**
     * Batch-create DOCUMENT topics (+ empty FILE drafts) from a CSV file.
     */
    KnowledgeImportResultVo importItems(KnowledgeImportOptionsDto options, MultipartFile file);

    /**
     * Batch-import FAQ topics from a CSV file.
     */
    KnowledgeImportResultVo importFaq(KnowledgeImportOptionsDto options, MultipartFile file);

    /**
     * Attach source files to existing DOCUMENT topics (PDF / Markdown / HTML).
     */
    KnowledgeImportResultVo importFiles(KnowledgeImportOptionsDto options, List<MultipartFile> files);
}