package com.g2rain.knowledge.api;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.dto.KnowledgeCategorySelectDto;
import com.g2rain.knowledge.vo.KnowledgeCategoryVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;


/**
 * 知识分类表API接口
 * 表名: knowledge_category
 *
 * @author G2rain Generator
 */
@Tag(name = "知识分类表", description = "知识分类表相关接口")
public interface KnowledgeCategoryApi {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return 数据列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询知识分类表列表", description = "根据查询条件返回知识分类表列表")
    Result<List<KnowledgeCategoryVo>> selectList(KnowledgeCategorySelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页数据
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询知识分类表列表", description = "分页查询知识分类表列表")
    Result<PageData<KnowledgeCategoryVo>> selectPage(PageSelectListDto<KnowledgeCategorySelectDto> selectDto);
}