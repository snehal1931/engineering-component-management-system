package com.edcms.dto.response;

import com.edcms.enums.VersionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * API response object for ComponentVersion data.
 *
 * <p>Includes denormalized component info (componentId, componentCode, componentName)
 * so the client doesn't need a separate call to identify which component this version belongs to.
 */
@Data
@Builder
public class ComponentVersionResponse {
    private Long id;
    private String versionNumber;
    private VersionStatus status;
    private String releaseNotes;

    // Denormalized parent component info — avoids extra API calls
    private Long componentId;
    private String componentCode;
    private String componentName;

    private Long createdById;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
