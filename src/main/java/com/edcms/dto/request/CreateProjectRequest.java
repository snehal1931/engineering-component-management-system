package com.edcms.dto.request;

import com.edcms.enums.ProjectStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for creating a new engineering project.
 */
@Data
public class CreateProjectRequest {

    @NotBlank(message = "Project name is required")
    @Size(min = 3, max = 200, message = "Project name must be between 3 and 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    /**
     * ID of the user creating this project.
     * In a full auth system, this would come from the JWT token.
     * For now, it is passed in the request body.
     */
    @NotNull(message = "Created-by user ID is required")
    private Long createdById;
}
