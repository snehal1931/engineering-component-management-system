package com.edcms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for declaring a compatibility rule between two component versions.
 *
 * <p>Rules are symmetric — (A,B) and (B,A) are treated as the same rule.
 * The service layer normalizes the pair by always storing the smaller ID as versionAId.
 */
@Data
public class CreateCompatibilityRuleRequest {

    @NotNull(message = "First component version ID (versionAId) is required")
    private Long versionAId;

    @NotNull(message = "Second component version ID (versionBId) is required")
    private Long versionBId;

    @NotNull(message = "isCompatible flag is required")
    private Boolean isCompatible;

    @NotBlank(message = "Rule description is required — explain WHY these versions are/are not compatible")
    @Size(min = 10, max = 2000, message = "Rule description must be between 10 and 2000 characters")
    private String ruleDescription;

    @NotNull(message = "Created-by user ID is required")
    private Long createdById;
}
