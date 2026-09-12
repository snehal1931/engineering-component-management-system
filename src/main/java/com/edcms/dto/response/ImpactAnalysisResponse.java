package com.edcms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response for the impact analysis endpoint.
 *
 * <p>Shows which assemblies are currently using a specific component version,
 * and optionally how many of them are active vs archived.
 *
 * <p>This helps engineers answer: "If I deprecate Battery v1.0, what breaks?"
 */
@Data
@Builder
public class ImpactAnalysisResponse {

    /** The component version being analyzed. */
    private Long componentVersionId;
    private String versionNumber;
    private String componentCode;
    private String componentName;

    /** Total number of assemblies currently using this version. */
    private int totalAffectedAssemblies;

    /** Detailed list of affected assemblies. */
    private List<AffectedAssemblyInfo> affectedAssemblies;

    /**
     * Lightweight info about each affected assembly.
     */
    @Data
    @Builder
    public static class AffectedAssemblyInfo {
        private Long assemblyId;
        private String assemblyName;
        private Long projectId;
        private String projectName;
        private LocalDateTime versionAddedAt;
    }
}
