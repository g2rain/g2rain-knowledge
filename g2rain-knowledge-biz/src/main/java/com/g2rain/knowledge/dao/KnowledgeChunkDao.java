package com.g2rain.knowledge.dao;

import com.g2rain.knowledge.dao.po.KnowledgeChunkPo;
import com.g2rain.knowledge.dto.KnowledgeChunkSelectDto;
import com.g2rain.data.isolation.annotations.DataIsolation;
import com.g2rain.data.isolation.annotations.IgnoreIsolation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 知识切片清单表数据访问接口
 * 表名: knowledge_chunk
 *
 * @author G2rain Generator
 */
@Mapper
@DataIsolation(organIdPropertyName = "organId", organIdColumnName = "organ_id")
public interface KnowledgeChunkDao {

    /**
     * 插入单条记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int insert(KnowledgeChunkPo entity);

    /**
     * 批量插入记录
     *
     * @param list 实体对象列表
     * @return 影响行数
     */
    int insertMultiple(List<KnowledgeChunkPo> list);

    /**
     * 根据 ID 更新记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int update(KnowledgeChunkPo entity);

    /**
     * 根据 ID 删除记录
     *
     * @param id 主键 ID
     * @return 影响行数
     */
    int delete(Long id);

    /**
     * 根据ID和Version更新记录（乐观锁更新）
     *
     * @param entity 实体对象（必须包含version字段）
     * @return 影响行数
     */
    int updateByVersion(KnowledgeChunkPo entity);

    /**
     * 根据 ID 查询记录
     *
     * @param id 主键 ID
     * @return 实体对象
     */
    KnowledgeChunkPo selectById(Long id);

    /**
     * 根据查询入参 DTO 筛选列表
     *
     * @param selectDto 查询条件 DTO
     * @return 实体对象列表
     */
    List<KnowledgeChunkPo> selectList(KnowledgeChunkSelectDto selectDto);

    /**
     * 插入单条记录（忽略数据隔离）
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    @IgnoreIsolation
    int insertWithoutIsolation(KnowledgeChunkPo entity);

    /**
     * 根据ID更新记录（忽略数据隔离）
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    @IgnoreIsolation
    int updateWithoutIsolation(KnowledgeChunkPo entity);

    /**
     * 根据ID查询记录（忽略数据隔离）
     *
     * @param id 主键 ID
     * @return 实体对象
     */
    @IgnoreIsolation
    KnowledgeChunkPo selectByIdWithoutIsolation(Long id);

    /**
     * 根据查询入参DTO筛选列表（忽略数据隔离）
     *
     * @param selectDto 查询条件 DTO
     * @return 实体对象列表
     */
    @IgnoreIsolation
    List<KnowledgeChunkPo> selectListWithoutIsolation(KnowledgeChunkSelectDto selectDto);

    /**
     * Marks all chunks of a revision as INDEXED after Milvus staged upsert succeeds.
     */
    int markIndexedByRevisionId(@Param("revisionId") Long revisionId, @Param("updateTime") LocalDateTime updateTime);
}
