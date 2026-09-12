package com.edcms.mapper;

import com.edcms.dto.response.ComponentResponse;
import com.edcms.entity.Component;
import com.edcms.entity.ElectricalComponent;
import com.edcms.entity.MechanicalComponent;
import com.edcms.entity.SoftwareComponent;
// Note: NOT importing org.springframework.stereotype.Component to avoid name clash
// with com.edcms.entity.Component. Using fully-qualified annotation instead.

/**
 * Converts {@link Component} entities (and their subtypes) to {@link ComponentResponse} DTOs.
 *
 * <p><b>Polymorphism in action:</b><br>
 * This mapper accepts a base {@code Component} reference and uses Java 21's
 * pattern matching {@code instanceof} to extract subtype-specific fields.
 *
 * <p>This demonstrates runtime polymorphism — the same method handles
 * all three component types transparently. The caller doesn't need to
 * know the concrete type.
 *
 * <p>Alternatively, this could use the Visitor pattern for strict OCP compliance,
 * but {@code instanceof} with sealed classes (or pattern matching) is idiomatic Java 21.
 */
@org.springframework.stereotype.Component
public class ComponentMapper {

    /**
     * Map any Component subtype to a flat ComponentResponse.
     *
     * <p>Uses Java 21 pattern matching instanceof to safely cast and extract
     * subtype-specific fields without explicit casting or ClassCastException risk.
     */
    public ComponentResponse toResponse(Component component) {
        if (component == null) return null;

        // NOTE: component.getComponentType() is mapped to the JPA @DiscriminatorColumn
        // with insertable=false/updatable=false. For JOINED inheritance, Hibernate does NOT
        // back-populate this field after save or findById — it uses the Java runtime class
        // to identify subtypes internally. We derive the type from instanceof instead,
        // which is always accurate and avoids this Hibernate quirk.
        com.edcms.enums.ComponentType resolvedType;
        if (component instanceof MechanicalComponent) {
            resolvedType = com.edcms.enums.ComponentType.MECHANICAL;
        } else if (component instanceof ElectricalComponent) {
            resolvedType = com.edcms.enums.ComponentType.ELECTRICAL;
        } else if (component instanceof SoftwareComponent) {
            resolvedType = com.edcms.enums.ComponentType.SOFTWARE;
        } else {
            resolvedType = component.getComponentType(); // fallback
        }

        ComponentResponse.ComponentResponseBuilder builder = ComponentResponse.builder()
                .id(component.getId())
                .componentCode(component.getComponentCode())
                .name(component.getName())
                .description(component.getDescription())
                .componentType(resolvedType)
                .manufacturer(component.getManufacturer())
                .status(component.getStatus())
                .createdById(component.getCreatedBy() != null ? component.getCreatedBy().getId() : null)
                .createdByUsername(component.getCreatedBy() != null ? component.getCreatedBy().getUsername() : null)
                .createdAt(component.getCreatedAt())
                .updatedAt(component.getUpdatedAt());

        // Java 21 pattern matching instanceof — no explicit cast needed
        if (component instanceof MechanicalComponent mc) {
            builder.material(mc.getMaterial())
                   .weightKg(mc.getWeightKg());

        } else if (component instanceof ElectricalComponent ec) {
            builder.voltageRating(ec.getVoltageRating())
                   .currentRating(ec.getCurrentRating())
                   .powerRatingW(ec.getPowerRatingW());

        } else if (component instanceof SoftwareComponent sc) {
            builder.programmingLang(sc.getProgrammingLang())
                   .licenseType(sc.getLicenseType());
        }

        return builder.build();
    }
}
