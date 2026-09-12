package com.edcms.dto.request;

import com.edcms.enums.ComponentType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Request body for creating any type of engineering component.
 *
 * <p><b>Design Decision — Flat DTO with optional type-specific fields:</b><br>
 * Rather than creating separate request DTOs for each component type
 * (CreateMechanicalComponentRequest, CreateElectricalComponentRequest, etc.),
 * we use a single flat DTO with optional type-specific fields.
 *
 * <p>Reasons:
 * <ol>
 *   <li>Simpler API contract — one endpoint, one DTO.</li>
 *   <li>Validation of type-specific fields is conditional and handled
 *       in the service layer (where business logic belongs).</li>
 *   <li>Jackson can deserialize all variants without extra configuration.</li>
 * </ol>
 *
 * <p>Alternative: Use Jackson's {@code @JsonSubTypes} with a discriminator field.
 * That approach is valid and more strictly typed, but adds significant boilerplate
 * for three subtypes in a student project.
 *
 * <p>The {@code componentType} field is required and tells the service layer
 * which entity subclass to instantiate.
 */
@Data
public class CreateComponentRequest {

    // ── Common Fields (required for all types) ───────────────────────────────

    @NotBlank(message = "Component code is required")
    @Size(max = 100, message = "Component code must not exceed 100 characters")
    @Pattern(
            regexp = "^[A-Z]{2,10}-[A-Z0-9]{2,20}-\\d{3,6}$",
            message = "Component code must follow the format: TYPE-NAME-NUMBER (e.g., MECH-MOTOR-001)"
    )
    private String componentCode;

    @NotBlank(message = "Component name is required")
    @Size(min = 2, max = 200, message = "Component name must be between 2 and 200 characters")
    private String name;

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

    @NotNull(message = "Component type is required (MECHANICAL, ELECTRICAL, or SOFTWARE)")
    private ComponentType componentType;

    @Size(max = 200, message = "Manufacturer name must not exceed 200 characters")
    private String manufacturer;

    @NotNull(message = "Created-by user ID is required")
    private Long createdById;

    // ── Mechanical-specific Fields (optional) ────────────────────────────────

    @Size(max = 100, message = "Material must not exceed 100 characters")
    private String material;

    @DecimalMin(value = "0.0", inclusive = false, message = "Weight must be positive")
    private BigDecimal weightKg;

    // ── Electrical-specific Fields (optional) ────────────────────────────────

    @DecimalMin(value = "0.0", inclusive = false, message = "Voltage rating must be positive")
    private BigDecimal voltageRating;

    @DecimalMin(value = "0.0", inclusive = false, message = "Current rating must be positive")
    private BigDecimal currentRating;

    @DecimalMin(value = "0.0", inclusive = false, message = "Power rating must be positive")
    private BigDecimal powerRatingW;

    // ── Software-specific Fields (optional) ──────────────────────────────────

    @Size(max = 50, message = "Programming language must not exceed 50 characters")
    private String programmingLang;

    @Size(max = 100, message = "License type must not exceed 100 characters")
    private String licenseType;
}
