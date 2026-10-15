package com.g2rain.knowledge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Restores an indexed historical revision that was previously deprecated.
 */
@Schema(description = "回滚知识版本请求")
public class KnowledgeReleaseRollbackDto extends KnowledgeReleasePublishDto {
}
