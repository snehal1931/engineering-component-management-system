package com.edcms.dto.response;

import com.edcms.enums.ComponentStatus;
import com.edcms.enums.ComponentType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * API response object for Component data (all subtypes).
 *
 * <p>Uses a flat structure with nullable type-specific fields.
 * The {@code componentType} field tells the client which type-specific
 * fields are populated.
 *
 * <p>The version list is NOT included here. Use GET /api/components/{id}/versions
 * to fetch the version history for a component.
 */
@Data
@Builder
public class ComponentResponse {

    // ── Common Fields ─────────────────────────────────────────────────────────
    private Long id;
    private String componentCode;
    private String name;
    private String description;
    private ComponentType componentType;
    private String manufacturer;
    private ComponentStatus status;
    private Long createdById;
    private String createdByUsername;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // ── Mechanical-specific (null for other types) ────────────────────────────
    private String material;
    private BigDecimal weightKg;

    // ── Electrical-specific (null for other types) ────────────────────────────
    private BigDecimal voltageRating;
    private BigDecimal currentRating;
    private BigDecimal powerRatingW;

    // ── Software-specific (null for other types) ──────────────────────────────
    private String programmingLang;
    private String licenseType;
}
