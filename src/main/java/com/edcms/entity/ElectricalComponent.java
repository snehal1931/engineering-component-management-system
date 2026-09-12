package com.edcms.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * An electrical or electronic component.
 *
 * <p>Examples: batteries, sensors, motor controllers, inverters, circuit boards,
 * temperature sensors, CAN bus transceivers.
 *
 * <p><b>OOP — Inheritance:</b>
 * Extends {@link Component} with electrical-domain attributes: voltage, current,
 * and power ratings. These fields are meaningless for mechanical or software
 * components, which is precisely why JOINED inheritance is appropriate —
 * they live in their own table rather than polluting the base table with NULLs.
 */
@Entity
@Table(name = "electrical_components")
@DiscriminatorValue("ELECTRICAL")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class ElectricalComponent extends Component {

    /**
     * Maximum rated voltage in Volts (V).
     * Example: 48.00 V for a high-voltage battery system.
     */
    @Column(name = "voltage_rating", precision = 10, scale = 2)
    private BigDecimal voltageRating;

    /**
     * Maximum rated current in Amperes (A).
     * Example: 200.00 A for a motor controller.
     */
    @Column(name = "current_rating", precision = 10, scale = 2)
    private BigDecimal currentRating;

    /**
     * Maximum power rating in Watts (W).
     * Often derivable from voltage × current, but stored explicitly
     * since datasheets publish it directly.
     * Example: 5000.00 W for a traction motor controller.
     */
    @Column(name = "power_rating_w", precision = 10, scale = 2)
    private BigDecimal powerRatingW;
}
