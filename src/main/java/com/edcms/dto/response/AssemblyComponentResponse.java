package com.edcms.dto.response;

import com.edcms.enums.VersionStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Represents one component-version entry in an assembly's component list.
 * Models a single Bill of Materials (BoM) line item.
 */
@Data
@Builder
public class AssemblyComponentResponse {
    private Long assemblyComponentId;
    private int quantity;
    private String notes;
    private LocalDateTime addedAt;

    // Version info
    private Long componentVersionId;
    private String versionNumber;
    private VersionStatus versionStatus;

    // Parent component info (denormalized for client convenience)
    private Long componentId;
    private String componentCode;
    private String componentName;
}
