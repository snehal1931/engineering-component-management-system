package com.edcms.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for creating a new assembly within a project.
 */
@Data
public class CreateAssemblyRequest {

    @NotBlank(message = "Assembly name is required")
    @Size(min = 2, max = 200, message = "Assembly name must be between 2 and 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Created-by user ID is required")
    private Long createdById;
}
