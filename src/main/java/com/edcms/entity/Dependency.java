package com.edcms.entity;

import com.edcms.enums.DependencyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Declares a dependency relationship between two component versions.
 *
 * <p>Examples:
 * <ul>
 *   <li>Motor v2.0 REQUIRES Controller v3.0</li>
 *   <li>Firmware v1.5 RECOMMENDS CAN Library v2.0</li>
 *   <li>Legacy Driver v1.0 CONFLICTS with New Protocol Stack v3.0</li>
 * </ul>
 *
 * <p><b>Graph Model:</b><br>
 * Dependencies form a directed graph where:
 * <ul>
 *   <li>Nodes are {@link ComponentVersion} instances</li>
 *   <li>Edges are {@link Dependency} instances (directional, source → target)</li>
 * </ul>
 *
 * <p>The {@code DependencyService} uses DFS traversal on this graph to detect
 * circular dependencies before any new dependency is persisted.
 *
 * <p><b>Constraints:</b>
 * <ul>
 *   <li>A version cannot depend on itself (enforced by service + CHECK hint in schema)</li>
 *   <li>The same (source, target) pair cannot be declared twice (unique constraint)</li>
 *   <li>Circular dependencies are prevented by DFS cycle detection</li>
 * </ul>
 *
 * <p><b>Relationship fields explained:</b>
 * <ul>
 *   <li>{@code sourceVersion} — The version that HAS a dependency ("Motor v2.0 requires...")</li>
 *   <li>{@code targetVersion} — The version that IS depended upon ("...Controller v3.0")</li>
 * </ul>
 */
@Entity
@Table(
        name = "dependencies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dependencies_source_target",
                        columnNames = {"source_version_id", "target_version_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The version that declares the dependency.
     * Example: Motor v2.0 (the one that REQUIRES something).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_version_id", nullable = false)
    private ComponentVersion sourceVersion;

    /**
     * The version that is being depended upon.
     * Example: Controller v3.0 (the one being REQUIRED).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_version_id", nullable = false)
    private ComponentVersion targetVersion;

    /**
     * The type of dependency relationship.
     * REQUIRES (hard), RECOMMENDS (soft), or CONFLICTS.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 20)
    @Builder.Default
    private DependencyType dependencyType = DependencyType.REQUIRES;

    /**
     * Optional human-readable description of why this dependency exists.
     * Example: "Motor v2.0 uses the CAN protocol interface defined in Controller v3.0's API."
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * The user who declared this dependency.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "Dependency{id=" + id + ", type=" + dependencyType + "}";
    }
}
