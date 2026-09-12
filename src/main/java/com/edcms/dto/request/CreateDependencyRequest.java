package com.edcms.dto.request;

import com.edcms.enums.DependencyType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for declaring a dependency between two component versions.
 *
 * <p>Business rules enforced in the service layer:
 * <ul>
 *   <li>{@code sourceVersionId} != {@code targetVersionId} (no self-dependencies).</li>
 *   <li>The (source, target) pair must not already exist.</li>
 *   <li>Adding this dependency must not create a cycle in the dependency graph.</li>
 * </ul>
 */
@Data
public class CreateDependencyRequest {

    @NotNull(message = "Source version ID is required")
    private Long sourceVersionId;

    @NotNull(message = "Target version ID is required")
    private Long targetVersionId;

    @NotNull(message = "Dependency type is required (REQUIRES, RECOMMENDS, or CONFLICTS)")
    private DependencyType dependencyType;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Created-by user ID is required")
    private Long createdById;
}
