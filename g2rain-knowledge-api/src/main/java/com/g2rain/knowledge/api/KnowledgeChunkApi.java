package com.g2rain.knowledge.api;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.dto.KnowledgeChunkSelectDto;
import com.g2rain.knowledge.vo.KnowledgeChunkVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


/**
 * 知识切片清单表API接口
 * 表名: knowledge_chunk
 *
 * @author G2rain Generator
 */
@Tag(name = "知识切片清单表", description = "知识切片清单表相关接口")
public interface KnowledgeChunkApi {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return 数据列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询知识切片清单表列表", description = "根据查询条件返回知识切片清单表列表")
    Result<List<KnowledgeChunkVo>> selectList(KnowledgeChunkSelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页数据
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询知识切片清单表列表", description = "分页查询知识切片清单表列表")
    Result<PageData<KnowledgeChunkVo>> selectPage(PageSelectListDto<KnowledgeChunkSelectDto> selectDto);
}