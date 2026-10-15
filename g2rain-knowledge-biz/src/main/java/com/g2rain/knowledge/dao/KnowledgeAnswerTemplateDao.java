package com.g2rain.knowledge.dao;

import com.g2rain.knowledge.dao.po.KnowledgeAnswerTemplatePo;
import com.g2rain.knowledge.dto.KnowledgeAnswerTemplateSelectDto;
import com.g2rain.data.isolation.annotations.DataIsolation;
import com.g2rain.data.isolation.annotations.IgnoreIsolation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 知识回复模板表数据访问接口
 * 表名: knowledge_answer_template
 *
 * @author G2rain Generator
 */
@Mapper
@DataIsolation(organIdPropertyName = "organId", organIdColumnName = "organ_id")
public interface KnowledgeAnswerTemplateDao {

    /**
     * 插入单条记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int insert(KnowledgeAnswerTemplatePo entity);

    /**
     * 批量插入记录
     *
     * @param list 实体对象列表
     * @return 影响行数
     */
    int insertMultiple(List<KnowledgeAnswerTemplatePo> list);

    /**
     * 根据 ID 更新记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int update(KnowledgeAnswerTemplatePo entity);

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
    int updateByVersion(KnowledgeAnswerTemplatePo entity);

    /**
     * 根据 ID 查询记录
     *
     * @param id 主键 ID
     * @return 实体对象
     */
    KnowledgeAnswerTemplatePo selectById(Long id);

    /**
     * 根据查询入参 DTO 筛选列表
     *
     * @param selectDto 查询条件 DTO
     * @return 实体对象列表
     */
    List<KnowledgeAnswerTemplatePo> selectList(KnowledgeAnswerTemplateSelectDto selectDto);

    /**
     * 插入单条记录（忽略数据隔离）
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    @IgnoreIsolation
    int insertWithoutIsolation(KnowledgeAnswerTemplatePo entity);

    /**
     * 根据ID更新记录（忽略数据隔离）
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    @IgnoreIsolation
    int updateWithoutIsolation(KnowledgeAnswerTemplatePo entity);

    /**
     * 根据ID查询记录（忽略数据隔离）
     *
     * @param id 主键 ID
     * @return 实体对象
     */
    @IgnoreIsolation
    KnowledgeAnswerTemplatePo selectByIdWithoutIsolation(Long id);

    /**
     * 根据查询入参DTO筛选列表（忽略数据隔离）
     *
     * @param selectDto 查询条件 DTO
     * @return 实体对象列表
     */
    @IgnoreIsolation
    List<KnowledgeAnswerTemplatePo> selectListWithoutIsolation(KnowledgeAnswerTemplateSelectDto selectDto);
}
