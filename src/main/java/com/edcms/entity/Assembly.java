package com.edcms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a named grouping of specific component versions within a project.
 *
 * <p>Example:
 * <pre>
 *   Project: "Electric Vehicle Prototype"
 *     └── Assembly: "Powertrain Assembly"
 *           ├── Motor v2.0
 *           ├── Battery v1.5
 *           └── Controller v3.0
 * </pre>
 *
 * <p><b>Composition over Inheritance:</b><br>
 * An Assembly does not "extend" or "inherit from" anything. Instead, it is
 * COMPOSED of {@link AssemblyComponent} records, each of which references a
 * specific {@link ComponentVersion}. This is the "has-a" relationship in action.
 *
 * <p><b>Why AssemblyComponent and not @ManyToMany:</b><br>
 * A pure {@code @ManyToMany} between Assembly and ComponentVersion would create
 * a simple join table with no extra data. But the assembly membership has its own
 * meaningful attributes: {@code quantity}, {@code notes}, {@code added_at}.
 * Therefore, we model the join as an explicit entity: {@link AssemblyComponent}.
 * This is a standard pattern known as a "junction entity" or "association entity."
 *
 * <p><b>Business rule enforced in service layer:</b><br>
 * Only APPROVED component versions may be added to an assembly.
 */
@Entity
@Table(name = "assemblies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Assembly {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable name for this assembly.
     * Example: "Powertrain Assembly", "Chassis Sub-Assembly", "Sensor Array"
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Optional description of what this assembly does or contains.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The project this assembly belongs to.
     * An assembly always belongs to exactly one project.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    /**
     * The user who created this assembly.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    /**
     * The specific component versions included in this assembly.
     *
     * <p>This is modeled as an explicit join entity ({@link AssemblyComponent})
     * rather than a bare @ManyToMany, because the relationship has its own
     * data (quantity, notes, addedAt).
     *
     * <p>{@code CascadeType.ALL} + {@code orphanRemoval = true} ensures that
     * removing an AssemblyComponent from this list also removes it from the database.
     */
    @OneToMany(mappedBy = "assembly", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<AssemblyComponent> assemblyComponents = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "Assembly{id=" + id + ", name='" + name + "'}";
    }
}
