package com.g2rain.knowledge.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.time.LocalDate;
import com.g2rain.common.json.ConditionalJsonIgnore;
import com.g2rain.common.json.AdminCompanyCondition;

/**
 * 知识版本表返回VO
 * 关联表名: knowledge_revision
 * 功能：封装接口返回数据，继承BaseVo复用基础字段逻辑，隔离数据库实体与前端展示层
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识版本表 VO")
public class KnowledgeRevisionVo extends BaseVo {

    /**
     * 乐观锁版本，供草稿 save / replace-file 使用
     */
    @Schema(description = "乐观锁版本，供草稿 save / replace-file 的 expectedVersion 使用")
    private Integer version;

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 知识主题标识
     */
    @Schema(description = "知识主题标识")
    private Long knowledgeId;

    /**
     * 版本号，如 v3 或 2026.10.03
     */
    @Schema(description = "版本号，如 v3 或 2026.10.03")
    private String revisionNo;

    /**
     * 来源类型[FILE:文件, FAQ:结构化问答]
     */
    @Schema(description = "来源类型[FILE:文件, FAQ:结构化问答]")
    private String sourceType;

    /**
     * 版本状态[DRAFT,PARSING,PARSE_FAILED,REVIEWING,INDEXING,INDEX_FAILED,READY_TO_PUBLISH,PUBLISHED,DEPRECATED,REJECTED]
     */
    @Schema(description = "版本状态[DRAFT,PARSING,PARSE_FAILED,REVIEWING,INDEXING,INDEX_FAILED,READY_TO_PUBLISH,PUBLISHED,DEPRECATED,REJECTED]")
    private String revisionStatus;

    /**
     * 净化后的原始文件名
     */
    @Schema(description = "净化后的原始文件名")
    private String sourceFileName;

    /**
     * 原始文件MIME类型
     */
    @Schema(description = "原始文件MIME类型")
    private String sourceContentType;

    /**
     * 受控本地存储相对路径（查询 VO 不返回，安全投影）
     */
    @Schema(description = "受控本地存储相对路径（查询不返回）", hidden = true)
    private String sourceStoragePath;

    /**
     * 原始文件SHA-256
     */
    @Schema(description = "原始文件SHA-256")
    private String sourceSha256;

    /**
     * 原始文件大小，单位字节
     */
    @Schema(description = "原始文件大小，单位字节")
    private Long sourceFileSize;

    /**
     * 解析器名称，如 Docling
     */
    @Schema(description = "解析器名称，如 Docling")
    private String parserName;

    /**
     * 解析器版本
     */
    @Schema(description = "解析器版本")
    private String parserVersion;

    /**
     * 解析服务任务标识（查询 VO 不返回，安全投影）
     */
    @Schema(description = "解析服务任务标识（查询不返回）", hidden = true)
    private String parserTaskId;

    /**
     * 结构化解析产物相对路径（查询 VO 不返回，安全投影）
     */
    @Schema(description = "结构化解析产物相对路径（查询不返回）", hidden = true)
    private String parsedStoragePath;

    /**
     * 切片数量
     */
    @Schema(description = "切片数量")
    private Integer chunkCount;

    /**
     * 索引状态[PENDING,INDEXING,INDEXED,FAILED,DELETING,DELETED]
     */
    @Schema(description = "索引状态[PENDING,INDEXING,INDEXED,FAILED,DELETING,DELETED]")
    private String indexStatus;

    /**
     * 向量配置键，如 qwen-4096-v1；与 collection/维度对应，审核通过后写入
     */
    @Schema(description = "向量配置键，如 qwen-4096-v1；与 collection/维度对应，审核通过后写入")
    private String embeddingProfile;

    /**
     * 外部向量模型标识
     */
    @Schema(description = "外部向量模型标识")
    private String embeddingModel;

    /**
     * 索引完成时间
     */
    @Schema(description = "索引完成时间")
    private LocalDateTime indexCompletedTime;

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
    @Schema(description = "审核意见")
    private String reviewComment;

    /**
     * 创建用户标识
     */
    @Schema(description = "创建用户标识")
    private Long createUserId;

    /**
     * FAQ标准问题，仅FAQ来源使用
     */
    @Schema(description = "FAQ标准问题，仅FAQ来源使用")
    private String faqQuestion;

    /**
     * FAQ标准答案，仅FAQ来源使用
     */
    @Schema(description = "FAQ标准答案，仅FAQ来源使用")
    private String faqAnswer;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    @Schema(description = "删除标识（0 未删除，1 已删除）", example = "false")
    @ConditionalJsonIgnore(adminCompany = AdminCompanyCondition.TRUE)
    private Boolean deleteFlag;
}