package com.edcms.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Full API response for an Assembly, including all its component versions.
 */
@Data
@Builder
public class AssemblyResponse {
    private Long id;
    private String name;
    private String description;

    // Project summary — avoid embedding the full project object
    private Long projectId;
    private String projectName;

    private List<AssemblyComponentResponse> components;

    private Long createdById;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
