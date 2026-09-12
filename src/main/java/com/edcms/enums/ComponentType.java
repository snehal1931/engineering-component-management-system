package com.edcms.enums;

/**
 * Classifies the physical/logical type of a component.
 *
 * <p>This enum drives the JPA JOINED inheritance discriminator on the
 * Component entity hierarchy. Each value corresponds to a concrete
 * subclass and a separate database table with type-specific attributes.
 *
 * <p>Discriminator values are stored as strings (not ordinals) so that
 * reordering enum values never corrupts persisted data.
 */
public enum ComponentType {

    /**
     * A physical, mechanical component (e.g., gears, chassis, motors).
     * Stored in the {@code mechanical_components} table.
     * Type-specific fields: material, weight_kg.
     */
    MECHANICAL,

    /**
     * An electrical/electronic component (e.g., sensors, batteries, controllers).
     * Stored in the {@code electrical_components} table.
     * Type-specific fields: voltage_rating, current_rating, power_rating_w.
     */
    ELECTRICAL,

    /**
     * A software component (e.g., firmware, embedded OS, control algorithms).
     * Stored in the {@code software_components} table.
     * Type-specific fields: programming_lang, license_type.
     */
    SOFTWARE
}
