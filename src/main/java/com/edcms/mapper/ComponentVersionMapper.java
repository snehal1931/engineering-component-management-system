package com.edcms.mapper;

import com.edcms.dto.response.ComponentVersionResponse;
import com.edcms.entity.ComponentVersion;
import org.springframework.stereotype.Component;

/**
 * Converts {@link ComponentVersion} entities to response DTOs.
 */
@Component
public class ComponentVersionMapper {

    public ComponentVersionResponse toResponse(ComponentVersion version) {
        if (version == null) return null;

        return ComponentVersionResponse.builder()
                .id(version.getId())
                .versionNumber(version.getVersionNumber())
                .status(version.getStatus())
                .releaseNotes(version.getReleaseNotes())
                .componentId(version.getComponent() != null ? version.getComponent().getId() : null)
                .componentCode(version.getComponent() != null ? version.getComponent().getComponentCode() : null)
                .componentName(version.getComponent() != null ? version.getComponent().getName() : null)
                .createdById(version.getCreatedBy() != null ? version.getCreatedBy().getId() : null)
                .createdByUsername(version.getCreatedBy() != null ? version.getCreatedBy().getUsername() : null)
                .createdAt(version.getCreatedAt())
                .updatedAt(version.getUpdatedAt())
                .build();
    }
}
