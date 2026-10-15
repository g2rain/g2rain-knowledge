package com.g2rain.knowledge.vo;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseVo;
import io.swagger.v3.oas.annotations.media.Schema;

import com.g2rain.common.json.ConditionalJsonIgnore;
import com.g2rain.common.json.AdminCompanyCondition;

/**
 * 知识切片清单表返回VO
 * 关联表名: knowledge_chunk
 * 功能：封装接口返回数据，继承BaseVo复用基础字段逻辑，隔离数据库实体与前端展示层
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识切片清单表 VO")
public class KnowledgeChunkVo extends BaseVo {

    /**
     * 机构标识
     */
    @Schema(description = "机构标识")
    private Long organId;

    /**
     * 知识版本标识
     */
    @Schema(description = "知识版本标识")
    private Long revisionId;

    /**
     * 版本内切片序号，从1开始
     */
    @Schema(description = "版本内切片序号，从1开始")
    private Integer chunkNo;

    /**
     * 父级切片序号
     */
    @Schema(description = "父级切片序号")
    private Integer parentChunkNo;

    /**
     * 解析产物内的受控定位符
     */
    @Schema(description = "解析产物内的受控定位符")
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
    @Schema(description = "切片正文SHA-256")
    private String contentSha256;

    /**
     * 切片状态[PENDING,INDEXED,FAILED,DELETED]
     */
    @Schema(description = "切片状态[PENDING,INDEXED,FAILED,DELETED]")
    private String chunkStatus;

    /**
     * 标题层级路径，不保存完整正文
     */
    @Schema(description = "标题层级路径，不保存完整正文")
    private String titlePath;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    @Schema(description = "删除标识（0 未删除，1 已删除）", example = "false")
    @ConditionalJsonIgnore(adminCompany = AdminCompanyCondition.TRUE)
    private Boolean deleteFlag;
}