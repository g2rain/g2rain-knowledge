package com.g2rain.knowledge.converter;

import com.g2rain.common.converter.CommonConverter;
import com.g2rain.knowledge.dao.po.KnowledgeRevisionPo;
import com.g2rain.knowledge.dto.KnowledgeRevisionDto;
import com.g2rain.knowledge.vo.KnowledgeRevisionVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

/**
 * 知识版本表转换器
 * 用于Po、Vo、Dto之间的相互转换
 * 表名: knowledge_revision
 *
 * @author G2rain Generator
 */
@Mapper(uses = CommonConverter.class)
public interface KnowledgeRevisionConverter {

    /**
     * 单例实例，通过 {@link Mappers#getMapper(Class)} 获取 MapStruct 自动生成的实现。
     */
    KnowledgeRevisionConverter INSTANCE = Mappers.getMapper(KnowledgeRevisionConverter.class);

    /**
     * Po -> Vo
     * 自动将 createTime 和 updateTime 从 {@link LocalDateTime} 转换为 {@link String}
     * 内部存储路径与解析任务标识不对外返回（安全投影）
     */
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "localDateTimeToString")
    @Mapping(target = "updateTime", source = "updateTime", qualifiedByName = "localDateTimeToString")
    @Mapping(target = "sourceStoragePath", ignore = true)
    @Mapping(target = "parsedStoragePath", ignore = true)
    @Mapping(target = "parserTaskId", ignore = true)
    KnowledgeRevisionVo po2vo(KnowledgeRevisionPo po);

    /**
     * Dto -> Po
     * 自动将 createTime 和 updateTime 从 {@link String} 转换为 {@link LocalDateTime}
     * 忽略 version 字段
     * 忽略 deleteFlag 字段
     */
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "deleteFlag", ignore = true)
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "stringToLocalDateTime")
    @Mapping(target = "updateTime", source = "updateTime", qualifiedByName = "stringToLocalDateTime")
    KnowledgeRevisionPo dto2po(KnowledgeRevisionDto dto);
}
