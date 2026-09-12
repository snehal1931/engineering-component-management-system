package com.edcms.entity;

import com.edcms.enums.ComponentStatus;
import com.edcms.enums.ComponentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for all engineering components in the EDCMS system.
 *
 * <p><b>OOP Concept — Inheritance + Abstraction:</b><br>
 * This class is declared {@code abstract} because a "generic component" cannot
 * exist on its own. Every component in engineering is specifically a mechanical,
 * electrical, or software component — it must be one of those types.
 * This mirrors the real world and prevents instantiation of a meaningless base type.
 *
 * <p><b>JPA Inheritance Strategy — JOINED:</b><br>
 * We use {@code InheritanceType.JOINED}, which means:
 * <ul>
 *   <li>All common fields (id, code, name, status) live in the {@code components} table.</li>
 *   <li>Type-specific fields live in separate subtype tables
 *       (mechanical_components, electrical_components, software_components).</li>
 *   <li>When loading a MechanicalComponent, JPA performs a JOIN between
 *       {@code components} and {@code mechanical_components}.</li>
 * </ul>
 *
 * <p><b>Why JOINED over alternatives:</b>
 * <ul>
 *   <li>{@code SINGLE_TABLE}: Fastest queries but puts all columns in one wide table
 *       with many NULLs — violates normalization.</li>
 *   <li>{@code TABLE_PER_CLASS}: Each table is self-contained but makes polymorphic
 *       queries (e.g., "find all ACTIVE components") require UNION ALL across tables.</li>
 *   <li>{@code JOINED} (chosen): Normalized, supports polymorphic queries cleanly,
 *       minor JOIN overhead is acceptable at this scale.</li>
 * </ul>
 *
 * <p><b>Discriminator:</b><br>
 * The {@code component_type} column stores the type name (e.g., "MECHANICAL")
 * so Hibernate knows which subclass to instantiate when loading a row.
 *
 * <p><b>Polymorphism:</b><br>
 * Code that works with {@code Component} references can handle any subtype
 * without knowing the exact type — e.g., listing all components of a project
 * or running impact analysis across all component versions.
 */
@Entity
@Table(
        name = "components",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_components_code", columnNames = "component_code")
        }
)
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "component_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public abstract class Component {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Globally unique, human-readable code for this component.
     * Example: "MECH-MOTOR-001", "ELEC-BATT-005", "SW-FW-CTRL-002"
     *
     * <p>This code is used in engineering documents and by engineers to reference
     * the component without relying on database IDs.
     */
    @Column(name = "component_code", nullable = false, length = 100)
    private String componentCode;

    /**
     * Full descriptive name of the component.
     * Example: "High-Torque Brushless Motor" or "48V Lithium Battery Pack"
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Optional detailed description of the component's purpose and specifications.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The type of this component — MECHANICAL, ELECTRICAL, or SOFTWARE.
     *
     * <p>This is the JPA discriminator value, stored as a string in the
     * {@code component_type} column of the {@code components} table.
     * It is also exposed as a field so it can be used in queries and API responses.
     *
     * <p>The value is set by each concrete subclass and should not be changed.
     * {@code insertable = false} and {@code updatable = false} because JPA
     * manages this column via the discriminator mechanism.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "component_type", nullable = false, insertable = false, updatable = false)
    private ComponentType componentType;

    /**
     * Optional manufacturer or supplier of this component.
     * Example: "Bosch", "Texas Instruments", "Custom In-House"
     */
    @Column(name = "manufacturer", length = 200)
    private String manufacturer;

    /**
     * Current operational status of the component (not its versions).
     * When a component is DEPRECATED, no new versions should be created.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ComponentStatus status = ComponentStatus.ACTIVE;

    /**
     * The user who created this component record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    /**
     * All versions of this component.
     *
     * <p><b>Composition:</b> A component is meaningfully composed of its versions.
     * Versions cannot exist independently — they always belong to a component.
     * {@code CascadeType.ALL} ensures that deleting a component also deletes
     * all its versions (with appropriate safeguards in the service layer).
     */
    @OneToMany(mappedBy = "component", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ComponentVersion> versions = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{id=" + id + ", code='" + componentCode + "', name='" + name + "'}";
    }
}
