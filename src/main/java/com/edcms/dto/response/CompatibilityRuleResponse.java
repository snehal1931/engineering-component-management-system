package com.edcms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API response for a CompatibilityRule between two component versions.
 */
@Data
@Builder
public class CompatibilityRuleResponse {
    private Long id;
    private boolean compatible;
    private String ruleDescription;
    private LocalDateTime createdAt;

    // Version A summary
    private Long versionAId;
    private String versionANumber;
    private String versionAComponentCode;

    // Version B summary
    private Long versionBId;
    private String versionBNumber;
    private String versionBComponentCode;
}
