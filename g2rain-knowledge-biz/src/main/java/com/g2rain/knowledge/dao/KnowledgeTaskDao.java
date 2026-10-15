package com.g2rain.knowledge.dao;

import com.g2rain.knowledge.dao.po.KnowledgeTaskPo;
import com.g2rain.knowledge.dto.KnowledgeTaskSelectDto;
import com.g2rain.data.isolation.annotations.DataIsolation;
import com.g2rain.data.isolation.annotations.IgnoreIsolation;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;
import java.time.LocalDateTime;
import org.apache.ibatis.annotations.Param;

/**
 * 知识解析与索引处理任务表数据访问接口
 * 表名: knowledge_task
 *
 * @author G2rain Generator
 */
@Mapper
@DataIsolation(organIdPropertyName = "organId", organIdColumnName = "organ_id")
public interface KnowledgeTaskDao {

    /**
     * 插入单条记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int insert(KnowledgeTaskPo entity);

    /**
     * 批量插入记录
     *
     * @param list 实体对象列表
     * @return 影响行数
     */
    int insertMultiple(List<KnowledgeTaskPo> list);

    /**
     * 根据 ID 更新记录
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    int update(KnowledgeTaskPo entity);

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
    int updateByVersion(KnowledgeTaskPo entity);

    /**
     * 根据 ID 查询记录
     *
     * @param id 主键 ID
     * @return 实体对象
     */
    KnowledgeTaskPo selectById(Long id);

    /**
     * 根据查询入参 DTO 筛选列表
     *
     * @param selectDto 查询条件 DTO
     * @return 实体对象列表
     */
    List<KnowledgeTaskPo> selectList(KnowledgeTaskSelectDto selectDto);

    /**
     * 插入单条记录（忽略数据隔离）
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    @IgnoreIsolation
    int insertWithoutIsolation(KnowledgeTaskPo entity);

    /**
     * 根据ID更新记录（忽略数据隔离）
     *
     * @param entity 实体对象
     * @return 影响行数
     */
    @IgnoreIsolation
    int updateWithoutIsolation(KnowledgeTaskPo entity);

    /**
     * 根据ID查询记录（忽略数据隔离）
     *
     * @param id 主键 ID
     * @return 实体对象
     */
    @IgnoreIsolation
    KnowledgeTaskPo selectByIdWithoutIsolation(Long id);

    /**
     * 根据查询入参DTO筛选列表（忽略数据隔离）
     *
     * @param selectDto 查询条件 DTO
     * @return 实体对象列表
     */
    @IgnoreIsolation
    List<KnowledgeTaskPo> selectListWithoutIsolation(KnowledgeTaskSelectDto selectDto);

    int claim(@Param("taskId") Long taskId, @Param("expectedVersion") Integer expectedVersion,
              @Param("workerId") String workerId, @Param("leaseExpireTime") LocalDateTime leaseExpireTime,
              @Param("now") LocalDateTime now);

    int complete(@Param("taskId") Long taskId, @Param("workerId") String workerId,
                 @Param("expectedVersion") Integer expectedVersion, @Param("now") LocalDateTime now);

    int fail(@Param("taskId") Long taskId, @Param("workerId") String workerId,
             @Param("expectedVersion") Integer expectedVersion, @Param("errorCode") String errorCode,
             @Param("errorSummary") String errorSummary, @Param("now") LocalDateTime now);

    int retryFailed(@Param("organId") Long organId, @Param("taskId") Long taskId,
                    @Param("expectedVersion") Integer expectedVersion, @Param("availableTime") LocalDateTime availableTime,
                    @Param("updateTime") LocalDateTime updateTime);

    int cancelPending(@Param("organId") Long organId, @Param("taskId") Long taskId,
                      @Param("expectedVersion") Integer expectedVersion, @Param("finishedTime") LocalDateTime finishedTime,
                      @Param("updateTime") LocalDateTime updateTime);
}
