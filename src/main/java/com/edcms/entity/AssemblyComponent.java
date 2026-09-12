package com.edcms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Represents the membership of a specific {@link ComponentVersion} in an {@link Assembly}.
 *
 * <p>This is an explicit "junction entity" (also called an association entity or link table).
 * It corresponds to the {@code assembly_components} table in the database.
 *
 * <p><b>Why not @ManyToMany? — Critical Interview Answer:</b><br>
 * Spring/JPA's {@code @ManyToMany} creates a bare join table with only two foreign key
 * columns. We cannot attach additional data (like {@code quantity} or {@code notes})
 * to a bare join table.
 *
 * <p>When a relationship has its own attributes, it must be modeled as a first-class
 * entity. This is a fundamental OOP/database design principle.
 *
 * <p>This entity demonstrates:
 * <ul>
 *   <li><b>Composition</b> — Assembly is composed of AssemblyComponent records.</li>
 *   <li><b>Association with attributes</b> — The link itself carries data.</li>
 *   <li><b>Real-world modeling</b> — In a Bill of Materials (BoM), each line item
 *       specifies which part, which version, and how many — exactly what this entity stores.</li>
 * </ul>
 *
 * <p><b>Unique constraint:</b> An assembly cannot include the same component version twice.
 * The constraint {@code (assembly_id, component_version_id)} prevents this at the DB level.
 */
@Entity
@Table(
        name = "assembly_components",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_assembly_components_assembly_version",
                        columnNames = {"assembly_id", "component_version_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssemblyComponent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The assembly that contains this component version.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "assembly_id", nullable = false)
    private Assembly assembly;

    /**
     * The specific version of the component included in this assembly.
     *
     * <p>Note: this is a reference to a SPECIFIC VERSION (e.g., Motor v2.0),
     * not just the generic component. This is critical for engineering reproducibility.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_version_id", nullable = false)
    private ComponentVersion componentVersion;

    /**
     * Number of units of this component version required in the assembly.
     * Example: 4 wheel bearing units in a Suspension Assembly.
     * Defaults to 1 for single-instance components.
     */
    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    /**
     * Optional engineering notes about this specific component's role in the assembly.
     * Example: "Installed on rear-left position. Torqued to 45 Nm."
     */
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    /**
     * Timestamp when this component version was added to the assembly.
     * Useful for audit trails.
     */
    @CreationTimestamp
    @Column(name = "added_at", nullable = false, updatable = false)
    private LocalDateTime addedAt;

    @Override
    public String toString() {
        return "AssemblyComponent{id=" + id
                + ", quantity=" + quantity + "}";
    }
}
