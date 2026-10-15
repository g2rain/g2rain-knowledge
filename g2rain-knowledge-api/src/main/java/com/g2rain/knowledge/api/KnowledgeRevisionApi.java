package com.g2rain.knowledge.api;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.dto.KnowledgeRevisionSelectDto;
import com.g2rain.knowledge.dto.KnowledgeReviewDto;
import com.g2rain.knowledge.vo.KnowledgeRevisionVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;


/**
 * 知识版本表API接口
 * 表名: knowledge_revision
 *
 * @author G2rain Generator
 */
@Tag(name = "知识版本表", description = "知识版本表相关接口")
public interface KnowledgeRevisionApi {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return 数据列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询知识版本表列表", description = "根据查询条件返回知识版本表列表")
    Result<List<KnowledgeRevisionVo>> selectList(KnowledgeRevisionSelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页数据
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询知识版本表列表", description = "分页查询知识版本表列表")
    Result<PageData<KnowledgeRevisionVo>> selectPage(PageSelectListDto<KnowledgeRevisionSelectDto> selectDto);

    @PostMapping("/review/submit")
    @Operation(summary = "提交知识版本审核")
    Result<Void> submitReview(@RequestBody KnowledgeReviewDto dto);

    @PostMapping("/review/approve")
    @Operation(summary = "通过原文档与标签审核")
    Result<Long> approveReview(@RequestBody KnowledgeReviewDto dto);

    @PostMapping("/review/reject")
    @Operation(summary = "拒绝原文档与标签审核")
    Result<Void> rejectReview(@RequestBody KnowledgeReviewDto dto);
}
