package com.edcms.dto.response;

import com.edcms.enums.ProjectStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * API response object for Project data.
 *
 * <p>Contains a summary of assemblies rather than the full assembly objects
 * to keep the response size manageable. The full assembly details are fetched
 * via the separate GET /api/assemblies/{id} endpoint.
 */
@Data
@Builder
public class ProjectResponse {
    private Long id;
    private String name;
    private String description;
    private ProjectStatus status;
    private Long createdById;
    private String createdByUsername;
    private List<AssemblySummaryResponse> assemblies;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
