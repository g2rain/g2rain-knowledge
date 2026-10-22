package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import com.g2rain.common.validation.CreateGroup;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 知识版本表查询DTO
 * 表名: knowledge_revision
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识版本表 DTO")
public class KnowledgeRevisionDto extends BaseDto {

    /**
     * 乐观锁期望版本（更新草稿必填；创建可省略）
     */
    @Schema(description = "乐观锁期望版本（更新草稿必填，与 replace-file 的 expectedVersion 语义一致）")
    private Integer version;

    /**
     * 机构标识（服务端填充）
     */
    @Schema(description = "机构标识（服务端填充，前端可省略）")
    private Long organId;

    /**
     * 知识主题标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "知识主题标识（新增必填）")
    private Long knowledgeId;

    /**
     * 版本号，如 v3 或 2026.10.03
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 64)
    @Schema(description = "版本号，如 v3 或 2026.10.03（新增必填）", maxLength = 64)
    private String revisionNo;

    /**
     * 来源类型[FILE:文件, FAQ:结构化问答]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "来源类型[FILE:文件, FAQ:结构化问答]（新增必填）", maxLength = 32)
    private String sourceType;

    /**
     * 版本状态（创建固定 DRAFT，只读）
     */
    @Size(max = 32)
    @Schema(description = "版本状态（服务端固定 DRAFT，前端可省略）", maxLength = 32)
    private String revisionStatus;

    /**
     * 净化后的原始文件名（只读，由 replace-file 写入）
     */
    @Size(max = 512)
    @Schema(description = "净化后的原始文件名（只读）", maxLength = 512)
    private String sourceFileName;

    /**
     * 原始文件MIME类型（只读）
     */
    @Size(max = 128)
    @Schema(description = "原始文件MIME类型（只读）", maxLength = 128)
    private String sourceContentType;

    /**
     * 受控本地存储相对路径（只读）
     */
    @Size(max = 1024)
    @Schema(description = "受控本地存储相对路径（只读）", maxLength = 1024)
    private String sourceStoragePath;

    /**
     * 原始文件SHA-256（只读）
     */
    @Size(max = 64)
    @Schema(description = "原始文件SHA-256（只读）", maxLength = 64)
    private String sourceSha256;

    /**
     * 原始文件大小，单位字节（只读）
     */
    @Schema(description = "原始文件大小，单位字节（只读）")
    private Long sourceFileSize;

    /**
     * 解析器名称（只读）
     */
    @Size(max = 64)
    @Schema(description = "解析器名称（只读）", maxLength = 64)
    private String parserName;

    /**
     * 解析器版本（只读）
     */
    @Size(max = 64)
    @Schema(description = "解析器版本（只读）", maxLength = 64)
    private String parserVersion;

    /**
     * 解析服务任务标识（只读）
     */
    @Size(max = 128)
    @Schema(description = "解析服务任务标识（只读）", maxLength = 128)
    private String parserTaskId;

    /**
     * 结构化解析产物相对路径（只读）
     */
    @Size(max = 1024)
    @Schema(description = "结构化解析产物相对路径（只读）", maxLength = 1024)
    private String parsedStoragePath;

    /**
     * 切片数量（创建固定 0，只读）
     */
    @Schema(description = "切片数量（服务端固定 0，前端可省略）")
    private Integer chunkCount;

    /**
     * 索引状态（创建固定 PENDING，只读）
     */
    @Size(max = 32)
    @Schema(description = "索引状态（服务端固定 PENDING，前端可省略）", maxLength = 32)
    private String indexStatus;

    /**
     * 向量配置键（只读）
     */
    @Size(max = 64)
    @Schema(description = "向量配置键（只读）", maxLength = 64)
    private String embeddingProfile;

    /**
     * 外部向量模型标识（只读）
     */
    @Size(max = 128)
    @Schema(description = "外部向量模型标识（只读）", maxLength = 128)
    private String embeddingModel;

    /**
     * 索引完成时间（只读）
     */
    @Schema(description = "索引完成时间（只读）")
    private LocalDateTime indexCompletedTime;

    /**
     * 审核用户标识（只读）
     */
    @Schema(description = "审核用户标识（只读）")
    private Long reviewerUserId;

    /**
     * 审核时间（只读）
     */
    @Schema(description = "审核时间（只读）")
    private LocalDateTime reviewTime;

    /**
     * 审核意见（只读）
     */
    @Size(max = 1024)
    @Schema(description = "审核意见（只读）", maxLength = 1024)
    private String reviewComment;

    /**
     * 创建用户标识（服务端填充）
     */
    @Schema(description = "创建用户标识（服务端填充，前端可省略）")
    private Long createUserId;

    /**
     * FAQ标准问题，仅FAQ来源使用
     */
    @Size(max = 65535)
    @Schema(description = "FAQ标准问题，仅FAQ来源使用", maxLength = 65535)
    private String faqQuestion;

    /**
     * FAQ标准答案，仅FAQ来源使用
     */
    @Size(max = 16777215)
    @Schema(description = "FAQ标准答案，仅FAQ来源使用", maxLength = 16777215)
    private String faqAnswer;
}
