package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.g2rain.common.validation.CreateGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识切片清单表查询DTO
 * 表名: knowledge_chunk
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识切片清单表 DTO")
public class KnowledgeChunkDto extends BaseDto {

    /**
     * 机构标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "机构标识（新增必填）")
    private Long organId;

    /**
     * 知识版本标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "知识版本标识（新增必填）")
    private Long revisionId;

    /**
     * 版本内切片序号，从1开始
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "版本内切片序号，从1开始（新增必填）")
    private Integer chunkNo;

    /**
     * 父级切片序号
     */
    @Schema(description = "父级切片序号")
    private Integer parentChunkNo;

    /**
     * 解析产物内的受控定位符
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 1024)
    @Schema(description = "解析产物内的受控定位符（新增必填）", maxLength = 1024)
    private String sourceLocator;

    /**
     * 起始页码
     */
    @Schema(description = "起始页码")
    private Integer pageStart;

    /**
     * 结束页码
     */
    @Schema(description = "结束页码")
    private Integer pageEnd;

    /**
     * 切片token数量
     */
    @Schema(description = "切片token数量")
    private Integer tokenCount;

    /**
     * 切片正文SHA-256
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 64)
    @Schema(description = "切片正文SHA-256（新增必填）", maxLength = 64)
    private String contentSha256;

    /**
     * 切片状态[PENDING,INDEXED,FAILED,DELETED]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "切片状态[PENDING,INDEXED,FAILED,DELETED]（新增必填）", maxLength = 32)
    private String chunkStatus;

    /**
     * 标题层级路径，不保存完整正文
     */
    @Size(max = 1073741824)
    @Schema(description = "标题层级路径，不保存完整正文", maxLength = 1073741824)
    private String titlePath;
}
