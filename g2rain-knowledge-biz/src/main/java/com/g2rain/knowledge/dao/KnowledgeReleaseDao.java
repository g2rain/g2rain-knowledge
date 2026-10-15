package com.g2rain.knowledge.dao;

import com.g2rain.knowledge.dao.po.KnowledgeReleasePo;
import com.g2rain.knowledge.dto.KnowledgeReleaseSelectDto;
import com.g2rain.data.isolation.annotations.DataIsolation;
import com.g2rain.data.isolation.annotations.IgnoreIsolation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 知识发布选择表数据访问接口
 * 表名: knowledge_release
 *
 * @author G2rain Generator
 */
@Mapper
@DataIsolation(organIdPropertyName = "organId", organIdColumnName = "organ_id")
public interface KnowledgeReleaseDao {

    /**
     * 插入单条记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int insert(KnowledgeReleasePo entity);

    /**
     * 批量插入记录
     *
     * @param list 实体对象列表
     * @return 影响行数
     */
    int insertMultiple(List<KnowledgeReleasePo> list);

    /**
     * 根据 ID 更新记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int update(KnowledgeReleasePo entity);

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
    int updateByVersion(KnowledgeReleasePo entity);

    /**
     * 根据 ID 查询记录
     *
     * @param id 主键 ID
     * @return 实体对象
     */
    KnowledgeReleasePo selectById(Long id);

    /**
     * 根据查询入参 DTO 筛选列表
     *
     * @param selectDto 查询条件 DTO
     * @return 实体对象列表
     */
    List<KnowledgeReleasePo> selectList(KnowledgeReleaseSelectDto selectDto);

    /**
     * 插入单条记录（忽略数据隔离）
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    @IgnoreIsolation
    int insertWithoutIsolation(KnowledgeReleasePo entity);

    /**
     * 根据ID更新记录（忽略数据隔离）
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    @IgnoreIsolation
    int updateWithoutIsolation(KnowledgeReleasePo entity);

    /**
     * 根据ID查询记录（忽略数据隔离）
     *
     * @param id 主键 ID
     * @return 实体对象
     */
    @IgnoreIsolation
    KnowledgeReleasePo selectByIdWithoutIsolation(Long id);

    /**
     * 根据查询入参DTO筛选列表（忽略数据隔离）
     *
     * @param selectDto 查询条件 DTO
     * @return 实体对象列表
     */
    @IgnoreIsolation
    List<KnowledgeReleasePo> selectListWithoutIsolation(KnowledgeReleaseSelectDto selectDto);
}
