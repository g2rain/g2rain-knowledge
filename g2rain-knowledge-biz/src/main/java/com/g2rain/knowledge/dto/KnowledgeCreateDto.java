package com.g2rain.knowledge.dto;

import com.g2rain.common.model.BaseDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Creates a knowledge topic. Tenant, status and creator are derived by the service.
 */
@Getter
@Setter
@Schema(description = "创建知识主题请求")
public class KnowledgeCreateDto extends BaseDto {
    @NotBlank
    @Size(max = 128)
    @Pattern(regexp = "[a-z][a-z0-9_]*")
    private String knowledgeKey;

    @NotBlank
    @Size(max = 256)
    private String knowledgeName;

    @NotBlank
    @Pattern(regexp = "DOCUMENT|FAQ")
    private String knowledgeType;

    private Long categoryId;

    @Size(max = 1024)
    private String description;

    private List<@Pattern(regexp = "[a-z0-9_]+") @Size(max = 64) String> tags;

    private Integer priority;
}
