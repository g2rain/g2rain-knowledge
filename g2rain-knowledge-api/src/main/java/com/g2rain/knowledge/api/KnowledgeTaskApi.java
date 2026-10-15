package com.g2rain.knowledge.api;

import com.g2rain.common.model.PageData;
import com.g2rain.common.model.PageSelectListDto;
import com.g2rain.common.model.Result;
import com.g2rain.knowledge.dto.KnowledgeTaskSelectDto;
import com.g2rain.knowledge.vo.KnowledgeTaskVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;


/**
 * 知识解析与索引处理任务表API接口
 * 表名: knowledge_task
 *
 * @author G2rain Generator
 */
@Tag(name = "知识解析与索引处理任务表", description = "知识解析与索引处理任务表相关接口")
public interface KnowledgeTaskApi {

    /**
     * 根据条件查询列表
     *
     * @param selectDto 查询条件 DTO
     * @return 数据列表
     */
    @GetMapping("/list")
    @Operation(summary = "查询知识解析与索引处理任务表列表", description = "根据查询条件返回知识解析与索引处理任务表列表")
    Result<List<KnowledgeTaskVo>> selectList(KnowledgeTaskSelectDto selectDto);

    /**
     * 根据条件分页查询
     *
     * @param selectDto 查询条件DTO（包含分页参数）
     * @return 分页数据
     */
    @GetMapping("/page")
    @Operation(summary = "分页查询知识解析与索引处理任务表列表", description = "分页查询知识解析与索引处理任务表列表")
    Result<PageData<KnowledgeTaskVo>> selectPage(PageSelectListDto<KnowledgeTaskSelectDto> selectDto);

    /**
     * 为一个知识版本发起处理链路。服务端根据版本当前状态创建或复用首个
     * PARSE 任务；解析完成后的切片、审核和索引任务由任务链自行推进。
     *
     * @param revisionId 知识版本标识
     * @return 首个处理任务标识
     */
    @PostMapping("/process/{revisionId}")
    @Operation(summary = "发起知识版本处理", description = "创建或复用知识版本的解析处理任务")
    Result<Long> processRevision(@PathVariable Long revisionId);
}
