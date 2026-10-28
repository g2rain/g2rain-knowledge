package com.g2rain.knowledge.dao.domain;

import com.g2rain.data.isolation.annotations.IgnoreIsolation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * DEV-004 public catalog reads: fixed PUBLIC + active published predicates; no organ isolation.
 */
@Mapper
public interface KnowledgePublicCatalogDomainDao {
    @IgnoreIsolation
    long countPublic(@Param("categoryId") Long categoryId, @Param("keyword") String keyword);

    @IgnoreIsolation
    List<KnowledgePublicCatalogRow> selectPublicPage(@Param("categoryId") Long categoryId,
                                                     @Param("keyword") String keyword,
                                                     @Param("offset") int offset,
                                                     @Param("limit") int limit);

    @IgnoreIsolation
    List<KnowledgePublicCategoryRow> selectPublicCategories();
}
