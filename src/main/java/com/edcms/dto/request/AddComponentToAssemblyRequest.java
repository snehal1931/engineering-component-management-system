package com.edcms.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for adding a specific component version to an assembly.
 *
 * <p>Business rules enforced in the service layer:
 * <ul>
 *   <li>The component version must have status APPROVED.</li>
 *   <li>The same component version cannot be added to the same assembly twice.</li>
 *   <li>Compatibility rules are checked against all existing versions in the assembly.</li>
 * </ul>
 */
@Data
public class AddComponentToAssemblyRequest {

    @NotNull(message = "Component version ID is required")
    private Long componentVersionId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;

    @Size(max = 1000, message = "Notes must not exceed 1000 characters")
    private String notes;
}
