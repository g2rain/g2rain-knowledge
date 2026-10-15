package com.g2rain.knowledge.converter;

import com.g2rain.common.converter.CommonConverter;
import com.g2rain.knowledge.dao.po.KnowledgeCategoryPo;
import com.g2rain.knowledge.dto.KnowledgeCategoryDto;
import com.g2rain.knowledge.vo.KnowledgeCategoryVo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

import java.time.LocalDateTime;

/**
 * 知识分类表转换器
 * 用于Po、Vo、Dto之间的相互转换
 * 表名: knowledge_category
 *
 * @author G2rain Generator
 */
@Mapper(uses = CommonConverter.class)
public interface KnowledgeCategoryConverter {

    /**
     * 单例实例，通过 {@link Mappers#getMapper(Class)} 获取 MapStruct 自动生成的实现。
     */
    KnowledgeCategoryConverter INSTANCE = Mappers.getMapper(KnowledgeCategoryConverter.class);

    /**
     * Po -> Vo
     * 自动将 createTime 和 updateTime 从 {@link LocalDateTime} 转换为 {@link String}
     */
    @Mapping(target = "createTime", source = "createTime", qualifiedByName = "localDateTimeToString")
    @Mapping(target = "updateTime", source = "updateTime", qualifiedByName = "localDateTimeToString")
    KnowledgeCategoryVo po2vo(KnowledgeCategoryPo po);

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
    KnowledgeCategoryPo dto2po(KnowledgeCategoryDto dto);
}
