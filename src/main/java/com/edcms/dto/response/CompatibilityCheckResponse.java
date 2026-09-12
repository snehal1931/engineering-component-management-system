package com.edcms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Response for the compatibility check operation.
 *
 * <p>If {@code allCompatible} is true, the provided versions can all be
 * used together in an assembly. Otherwise, {@code violations} lists each
 * incompatible pair and the reason declared in the compatibility rule.
 */
@Data
@Builder
public class CompatibilityCheckResponse {

    /** True if all provided versions are mutually compatible. */
    private boolean allCompatible;

    /** Human-readable summary message. */
    private String message;

    /** List of specific incompatibility violations found. Empty if allCompatible = true. */
    private List<IncompatiblePair> violations;

    /**
     * Represents one incompatible pair found during the check.
     */
    @Data
    @Builder
    public static class IncompatiblePair {
        private Long versionAId;
        private String versionADescription;   // e.g., "ELEC-MOTOR-001 v2.0"
        private Long versionBId;
        private String versionBDescription;   // e.g., "ELEC-CTRL-001 v2.0"
        private String reason;                // From CompatibilityRule.ruleDescription
    }
}
