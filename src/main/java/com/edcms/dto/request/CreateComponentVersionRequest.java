package com.edcms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for creating a new version of an existing component.
 *
 * <p>Version numbers must follow a simple major.minor[.patch] format.
 * The regex allows: "1.0", "2.3", "10.5.1", etc.
 *
 * <p>Business rule: The new version is always created in DRAFT status.
 * Status progression (DRAFT → APPROVED → DEPRECATED) happens via
 * separate dedicated endpoints.
 */
@Data
public class CreateComponentVersionRequest {

    @NotBlank(message = "Version number is required")
    @Pattern(
            regexp = "^\\d+\\.\\d+(\\.\\d+)?$",
            message = "Version number must follow semantic versioning format: MAJOR.MINOR or MAJOR.MINOR.PATCH (e.g., 1.0, 2.3.1)"
    )
    @Size(max = 20, message = "Version number must not exceed 20 characters")
    private String versionNumber;

    @Size(max = 5000, message = "Release notes must not exceed 5000 characters")
    private String releaseNotes;

    @NotNull(message = "Created-by user ID is required")
    private Long createdById;
}
