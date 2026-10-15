package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.LocalDate;
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
     * 机构标识
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "机构标识（新增必填）")
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
     * 版本状态[DRAFT,PARSING,PARSE_FAILED,REVIEWING,INDEXING,INDEX_FAILED,READY_TO_PUBLISH,PUBLISHED,DEPRECATED,REJECTED]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "版本状态[DRAFT,PARSING,PARSE_FAILED,REVIEWING,INDEXING,INDEX_FAILED,READY_TO_PUBLISH,PUBLISHED,DEPRECATED,REJECTED]（新增必填）", maxLength = 32)
    private String revisionStatus;

    /**
     * 净化后的原始文件名
     */
    @Size(max = 512)
    @Schema(description = "净化后的原始文件名", maxLength = 512)
    private String sourceFileName;

    /**
     * 原始文件MIME类型
     */
    @Size(max = 128)
    @Schema(description = "原始文件MIME类型", maxLength = 128)
    private String sourceContentType;

    /**
     * 受控本地存储相对路径
     */
    @Size(max = 1024)
    @Schema(description = "受控本地存储相对路径", maxLength = 1024)
    private String sourceStoragePath;

    /**
     * 原始文件SHA-256
     */
    @Size(max = 64)
    @Schema(description = "原始文件SHA-256", maxLength = 64)
    private String sourceSha256;

    /**
     * 原始文件大小，单位字节
     */
    @Schema(description = "原始文件大小，单位字节")
    private Long sourceFileSize;

    /**
     * 解析器名称，如 Docling
     */
    @Size(max = 64)
    @Schema(description = "解析器名称，如 Docling", maxLength = 64)
    private String parserName;

    /**
     * 解析器版本
     */
    @Size(max = 64)
    @Schema(description = "解析器版本", maxLength = 64)
    private String parserVersion;

    /**
     * 解析服务任务标识
     */
    @Size(max = 128)
    @Schema(description = "解析服务任务标识", maxLength = 128)
    private String parserTaskId;

    /**
     * 结构化解析产物相对路径
     */
    @Size(max = 1024)
    @Schema(description = "结构化解析产物相对路径", maxLength = 1024)
    private String parsedStoragePath;

    /**
     * 切片数量
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "切片数量（新增必填）")
    private Integer chunkCount;

    /**
     * 索引状态[PENDING,INDEXING,INDEXED,FAILED,DELETING,DELETED]
     */
    @NotBlank(groups = CreateGroup.class)
    @Size(max = 32)
    @Schema(description = "索引状态[PENDING,INDEXING,INDEXED,FAILED,DELETING,DELETED]（新增必填）", maxLength = 32)
    private String indexStatus;

    /**
     * 向量配置键，如 qwen-4096-v1；与 collection/维度对应，审核通过后写入
     */
    @Size(max = 64)
    @Schema(description = "向量配置键，如 qwen-4096-v1；与 collection/维度对应，审核通过后写入", maxLength = 64)
    private String embeddingProfile;

    /**
     * 外部向量模型标识
     */
    @Size(max = 128)
    @Schema(description = "外部向量模型标识", maxLength = 128)
    private String embeddingModel;

    /**
     * 索引完成时间
     */
    @Schema(description = "索引完成时间")
    private LocalDateTime indexCompletedTime;

    /**
     * 运营与同主题并列排序优先级，数值越大越靠前；不写入Milvus过滤
     */
    @NotNull(groups = CreateGroup.class)
    @Schema(description = "运营与同主题并列排序优先级，数值越大越靠前；不写入Milvus过滤（新增必填）")
    private Integer priority;

    /**
     * 审核用户标识
     */
    @Schema(description = "审核用户标识")
    private Long reviewerUserId;

    /**
     * 审核时间
     */
    @Schema(description = "审核时间")
    private LocalDateTime reviewTime;

    /**
     * 审核意见
     */
    @Size(max = 1024)
    @Schema(description = "审核意见", maxLength = 1024)
    private String reviewComment;

    /**
     * 创建用户标识
     */
    @Schema(description = "创建用户标识")
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

    /**
     * 版本已选标签编码数组，INDEX任务同步到Milvus Entity tags字段
     */
    @Size(max = 1073741824)
    @Schema(description = "版本已选标签编码数组，INDEX任务同步到Milvus Entity tags字段", maxLength = 1073741824)
    private String tagsJson;
}
