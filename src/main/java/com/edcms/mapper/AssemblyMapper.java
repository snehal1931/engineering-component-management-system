package com.edcms.mapper;

import com.edcms.dto.response.AssemblyComponentResponse;
import com.edcms.dto.response.AssemblyResponse;
import com.edcms.entity.Assembly;
import com.edcms.entity.AssemblyComponent;
import com.edcms.entity.ComponentVersion;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Converts {@link Assembly} and {@link AssemblyComponent} entities to response DTOs.
 */
@Component
public class AssemblyMapper {

    public AssemblyResponse toResponse(Assembly assembly) {
        if (assembly == null) return null;

        List<AssemblyComponentResponse> componentResponses = assembly.getAssemblyComponents() == null
                ? Collections.emptyList()
                : assembly.getAssemblyComponents().stream()
                        .map(this::toAssemblyComponentResponse)
                        .toList();

        return AssemblyResponse.builder()
                .id(assembly.getId())
                .name(assembly.getName())
                .description(assembly.getDescription())
                .projectId(assembly.getProject() != null ? assembly.getProject().getId() : null)
                .projectName(assembly.getProject() != null ? assembly.getProject().getName() : null)
                .components(componentResponses)
                .createdById(assembly.getCreatedBy() != null ? assembly.getCreatedBy().getId() : null)
                .createdByUsername(assembly.getCreatedBy() != null ? assembly.getCreatedBy().getUsername() : null)
                .createdAt(assembly.getCreatedAt())
                .updatedAt(assembly.getUpdatedAt())
                .build();
    }

    public AssemblyComponentResponse toAssemblyComponentResponse(AssemblyComponent ac) {
        if (ac == null) return null;

        ComponentVersion version = ac.getComponentVersion();
        com.edcms.entity.Component component = version != null ? version.getComponent() : null;

        return AssemblyComponentResponse.builder()
                .assemblyComponentId(ac.getId())
                .quantity(ac.getQuantity())
                .notes(ac.getNotes())
                .addedAt(ac.getAddedAt())
                .componentVersionId(version != null ? version.getId() : null)
                .versionNumber(version != null ? version.getVersionNumber() : null)
                .versionStatus(version != null ? version.getStatus() : null)
                .componentId(component != null ? component.getId() : null)
                .componentCode(component != null ? component.getComponentCode() : null)
                .componentName(component != null ? component.getName() : null)
                .build();
    }
}
