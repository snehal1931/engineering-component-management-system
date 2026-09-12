package com.edcms.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

/**
 * Request body for the compatibility check endpoint.
 *
 * <p>Provide a list of component version IDs to check if they can all
 * coexist in the same assembly without violating any compatibility rules.
 */
@Data
public class CompatibilityCheckRequest {

    @NotEmpty(message = "At least two component version IDs are required for a compatibility check")
    private List<Long> componentVersionIds;
}
