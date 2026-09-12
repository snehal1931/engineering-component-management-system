package com.edcms.entity;

import com.edcms.enums.VersionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a specific, immutable version of an engineering component.
 *
 * <p>Example: "Motor" component has versions v1.0, v2.0, v3.0.
 * Each version is an independent record with its own status and release notes.
 *
 * <p><b>Why versions are first-class entities (not just a string field):</b><br>
 * In engineering systems, components evolve over time. Assemblies must reference
 * a <em>specific</em> version, not just a generic component, because:
 * <ul>
 *   <li>Different versions may have different compatibility requirements.</li>
 *   <li>Approved assemblies must be reproducible — using "latest" is dangerous.</li>
 *   <li>Deprecating a version must trigger impact analysis on existing assemblies.</li>
 * </ul>
 *
 * <p><b>Version State Machine:</b>
 * <pre>
 *   DRAFT ──► APPROVED ──► DEPRECATED
 *     └──────────────────────────┘ (direct DRAFT→DEPRECATED allowed)
 *
 *   Invalid: DEPRECATED → APPROVED, DEPRECATED → DRAFT, APPROVED → DRAFT
 * </pre>
 * State transitions are enforced by {@code ComponentVersionService.validateTransition()}.
 *
 * <p><b>Composition:</b> A ComponentVersion cannot exist without a parent Component.
 * This is modeled as a non-null {@code @ManyToOne} with {@code CascadeType.ALL}
 * on the parent side.
 *
 * <p><b>Unique constraint:</b> (component_id, version_number) — a component cannot
 * have two versions with the same version number (e.g., two "v2.0" entries).
 */
@Entity
@Table(
        name = "component_versions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_component_versions_comp_version",
                        columnNames = {"component_id", "version_number"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComponentVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The parent component this version belongs to.
     * Cannot be null — every version must belong to exactly one component.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_id", nullable = false)
    private Component component;

    /**
     * Human-readable version identifier.
     * Convention: semantic versioning (e.g., "1.0", "2.3", "10.0.1").
     *
     * <p>Validated with a regex pattern in the DTO layer before being saved.
     * Example valid values: "1.0", "2.0.1", "10.5"
     */
    @Column(name = "version_number", nullable = false, length = 20)
    private String versionNumber;

    /**
     * Description of what changed in this version.
     * Example: "Increased torque output by 15%. Fixed thermal throttling bug."
     */
    @Column(name = "release_notes", columnDefinition = "TEXT")
    private String releaseNotes;

    /**
     * Current lifecycle status of this version.
     *
     * <p>Only {@code APPROVED} versions may be added to production assemblies.
     * The status defaults to {@code DRAFT} on creation and progresses via
     * the state machine enforced in the service layer.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private VersionStatus status = VersionStatus.DRAFT;

    /**
     * The user who created this version record.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    /**
     * All dependency declarations where THIS version is the source (dependent side).
     * Example: Motor v2.0 (this) → REQUIRES → Controller v3.0 (target).
     */
    @OneToMany(mappedBy = "sourceVersion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Dependency> outgoingDependencies = new ArrayList<>();

    /**
     * All dependency declarations where THIS version is the target (depended-upon side).
     * Example: Controller v3.0 (this) ← REQUIRES ← Motor v2.0 (source).
     */
    @OneToMany(mappedBy = "targetVersion", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Dependency> incomingDependencies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "ComponentVersion{id=" + id + ", versionNumber='" + versionNumber + "', status=" + status + "}";
    }
}
