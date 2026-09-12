package com.edcms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * A physically manufactured, non-electronic component.
 *
 * <p>Examples: motors, gears, chassis frames, bearings, drive shafts, seals.
 *
 * <p><b>OOP — Inheritance:</b>
 * This class extends {@link Component} and adds mechanical-specific attributes.
 * It is persisted in the {@code mechanical_components} table alongside
 * the shared fields in {@code components} (JOINED strategy).
 *
 * <p>The discriminator value {@code "MECHANICAL"} matches the {@link com.edcms.enums.ComponentType#MECHANICAL}
 * enum name, which is stored in the {@code component_type} column.
 *
 * <p><b>When to use inheritance (interview answer):</b><br>
 * Inheritance is used here because MechanicalComponent genuinely "is-a" Component
 * with additional domain-specific attributes (material, weight). It is NOT just
 * a tag or category — it represents a fundamentally different kind of entity
 * with different physical characteristics that need to be stored and queried.
 */
@Entity
@Table(name = "mechanical_components")
@DiscriminatorValue("MECHANICAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class MechanicalComponent extends Component {

    /**
     * Primary material the component is made of.
     * Example: "Aluminium Alloy 6061", "Carbon Fibre Composite", "Stainless Steel 316"
     */
    @Column(name = "material", length = 100)
    private String material;

    /**
     * Mass of the component in kilograms.
     * Stored with high precision for weight budget calculations.
     * Example: 2.3500 kg
     */
    @Column(name = "weight_kg", precision = 10, scale = 4)
    private BigDecimal weightKg;
}
