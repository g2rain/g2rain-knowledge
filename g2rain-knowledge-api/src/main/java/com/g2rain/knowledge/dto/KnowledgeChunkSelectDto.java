package com.g2rain.knowledge.dto;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import com.g2rain.common.model.BaseSelectListDto;
import io.swagger.v3.oas.annotations.media.Schema;


/**
 * 知识切片清单表查询入参DTO
 * 用于KnowledgeChunkDao.selectList方法的条件筛选
 * 表名: knowledge_chunk
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Schema(description = "知识切片清单表查询入参 DTO")
public class KnowledgeChunkSelectDto extends BaseSelectListDto {

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
}