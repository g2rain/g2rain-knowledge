package com.g2rain.knowledge.dao.po;

import com.g2rain.common.model.BasePo;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


/**
 * 知识切片清单表返回Po
 * 关联表名: knowledge_chunk
 * 功能：封装实体数据，继承BasePo复用基础字段逻辑
 *
 * @author G2rain Generator
 */
@Setter
@Getter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class KnowledgeChunkPo extends BasePo {

    /**
     * 机构标识
     */
    private Long organId;

    /**
     * 知识版本标识
     */
    private Long revisionId;

    /**
     * 版本内切片序号，从1开始
     */
    private Integer chunkNo;

    /**
     * 父级切片序号
     */
    private Integer parentChunkNo;

    /**
     * 解析产物内的受控定位符
     */
    private String sourceLocator;

    /**
     * 起始页码
     */
    private Integer pageStart;

    /**
     * 结束页码
     */
    private Integer pageEnd;

    /**
     * 切片token数量
     */
    private Integer tokenCount;

    /**
     * 切片正文SHA-256
     */
    private String contentSha256;

    /**
     * 切片状态[PENDING,INDEXED,FAILED,DELETED]
     */
    private String chunkStatus;

    /**
     * 标题层级路径，不保存完整正文
     */
    private String titlePath;

    /**
     * 删除标识[0:未删除, 1:已删除]
     */
    private Boolean deleteFlag;
}