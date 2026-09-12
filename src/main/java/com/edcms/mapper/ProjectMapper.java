package com.edcms.mapper;

import com.edcms.dto.response.AssemblySummaryResponse;
import com.edcms.dto.response.ProjectResponse;
import com.edcms.entity.Assembly;
import com.edcms.entity.Project;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * Converts between {@link Project} entities and response DTOs.
 */
@Component
public class ProjectMapper {

    public ProjectResponse toResponse(Project project) {
        if (project == null) return null;

        List<AssemblySummaryResponse> assemblySummaries = project.getAssemblies() == null
                ? Collections.emptyList()
                : project.getAssemblies().stream()
                        .map(this::toAssemblySummary)
                        .toList();

        return ProjectResponse.builder()
                .id(project.getId())
                .name(project.getName())
                .description(project.getDescription())
                .status(project.getStatus())
                .createdById(project.getCreatedBy() != null ? project.getCreatedBy().getId() : null)
                .createdByUsername(project.getCreatedBy() != null ? project.getCreatedBy().getUsername() : null)
                .assemblies(assemblySummaries)
                .createdAt(project.getCreatedAt())
                .updatedAt(project.getUpdatedAt())
                .build();
    }

    public AssemblySummaryResponse toAssemblySummary(Assembly assembly) {
        if (assembly == null) return null;
        return AssemblySummaryResponse.builder()
                .id(assembly.getId())
                .name(assembly.getName())
                .description(assembly.getDescription())
                .componentCount(
                        assembly.getAssemblyComponents() == null ? 0
                                : assembly.getAssemblyComponents().size()
                )
                .build();
    }
}
