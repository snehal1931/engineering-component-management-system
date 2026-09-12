package com.edcms.dto.response;

import com.edcms.enums.DependencyType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API response for a Dependency relationship between two component versions.
 */
@Data
@Builder
public class DependencyResponse {
    private Long id;
    private DependencyType dependencyType;
    private String description;
    private LocalDateTime createdAt;

    // Source version summary
    private Long sourceVersionId;
    private String sourceVersionNumber;
    private String sourceComponentCode;
    private String sourceComponentName;

    // Target version summary
    private Long targetVersionId;
    private String targetVersionNumber;
    private String targetComponentCode;
    private String targetComponentName;
}
