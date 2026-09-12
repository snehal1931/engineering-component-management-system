package com.edcms.repository;

import com.edcms.entity.ComponentVersion;
import com.edcms.entity.Dependency;
import com.edcms.enums.DependencyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Dependency} entities.
 *
 * <p>The DependencyService uses this repository to build the dependency graph
 * for DFS-based circular dependency detection.
 */
@Repository
public interface DependencyRepository extends JpaRepository<Dependency, Long> {

    /**
     * Get all dependencies where the given version is the SOURCE (the requirer).
     * Example: for "Motor v2.0", returns all things Motor v2.0 requires.
     * Used in DFS traversal for circular dependency detection.
     */
    List<Dependency> findBySourceVersion(ComponentVersion sourceVersion);

    /** Get by source version ID — avoids loading the full entity in the DFS loop. */
    List<Dependency> findBySourceVersionId(Long sourceVersionId);

    /**
     * Get all dependencies where the given version is the TARGET (the depended-upon).
     * Example: for "Controller v3.0", returns all versions that require it.
     * Used for impact analysis of "what depends on this version?"
     */
    List<Dependency> findByTargetVersion(ComponentVersion targetVersion);

    /**
     * Check if a dependency already exists between two specific versions.
     * Used to prevent duplicate dependency declarations.
     */
    boolean existsBySourceVersionAndTargetVersion(ComponentVersion sourceVersion, ComponentVersion targetVersion);

    /**
     * Find a specific dependency record between two versions.
     */
    Optional<Dependency> findBySourceVersionAndTargetVersion(ComponentVersion sourceVersion, ComponentVersion targetVersion);

    /** Filter dependencies by type. */
    List<Dependency> findBySourceVersionAndDependencyType(ComponentVersion sourceVersion, DependencyType type);
}
