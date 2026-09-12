package com.edcms.dto.response;

import lombok.Builder;
import lombok.Data;

/**
 * Lightweight assembly reference, embedded inside ProjectResponse.
 * Use GET /api/assemblies/{id} for full assembly details.
 */
@Data
@Builder
public class AssemblySummaryResponse {
    private Long id;
    private String name;
    private String description;
    private int componentCount;
}
