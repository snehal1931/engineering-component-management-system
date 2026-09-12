package com.edcms.dto.request;

import com.edcms.enums.ProjectStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request body for changing a project's lifecycle status.
 *
 * <p>Separated from the general update request because status change
 * is a distinct business operation with its own validation rules
 * (invalid transitions are rejected by the service layer).
 */
@Data
public class ChangeProjectStatusRequest {

    @NotNull(message = "New status is required")
    private ProjectStatus status;
}
