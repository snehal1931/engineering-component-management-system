package com.edcms.entity;

import com.edcms.enums.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a top-level engineering project in the EDCMS system.
 *
 * <p>A project is the root container in the domain hierarchy:
 * <pre>
 *   Project
 *     └── Assembly (many)
 *           └── AssemblyComponent (many) → ComponentVersion
 * </pre>
 *
 * <p><b>Composition:</b> A Project does not "inherit" anything — it is composed
 * of Assemblies. This is a "has-a" relationship, not an "is-a" relationship,
 * so composition (via {@code @OneToMany}) is the correct design.
 *
 * <p><b>Lifecycle:</b> Projects follow the status transitions:
 * {@code PLANNING → ACTIVE → COMPLETED → ARCHIVED}.
 * These transitions are enforced in {@code ProjectService}.
 */
@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Human-readable project name.
     * Example: "Electric Vehicle Prototype — Phase 1"
     */
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    /**
     * Optional detailed description of the project's purpose and scope.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Current lifecycle status of the project.
     * Stored as a string for database readability.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ProjectStatus status = ProjectStatus.PLANNING;

    /**
     * The user who created this project.
     *
     * <p>Many projects can be created by one user.
     * {@code FetchType.LAZY} — we don't need the full User object every time
     * we load a Project. Only fetch when explicitly accessed.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    /**
     * All assemblies that belong to this project.
     *
     * <p>{@code CascadeType.ALL} means that if a project is deleted,
     * all its assemblies are also deleted.
     *
     * <p>{@code orphanRemoval = true} ensures that if an Assembly is removed
     * from this list, it is also deleted from the database.
     */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<Assembly> assemblies = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Override
    public String toString() {
        return "Project{id=" + id + ", name='" + name + "', status=" + status + "}";
    }
}
