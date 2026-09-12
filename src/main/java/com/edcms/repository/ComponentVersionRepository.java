package com.edcms.repository;

import com.edcms.entity.Component;
import com.edcms.entity.ComponentVersion;
import com.edcms.enums.VersionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link ComponentVersion} entities.
 */
@Repository
public interface ComponentVersionRepository extends JpaRepository<ComponentVersion, Long> {

    /** Get all versions for a given component. */
    List<ComponentVersion> findByComponent(Component component);

    /** Get all versions for a component by the component's ID. */
    List<ComponentVersion> findByComponentId(Long componentId);

    /**
     * Find a specific version by component and version number.
     * Used to prevent duplicate version numbers per component.
     * Example: check if "Motor" already has "v2.0" before creating it.
     */
    Optional<ComponentVersion> findByComponentAndVersionNumber(Component component, String versionNumber);

    /** Check if a version number already exists for a component. */
    boolean existsByComponentAndVersionNumber(Component component, String versionNumber);

    /** Get all versions of a component with a specific status. */
    List<ComponentVersion> findByComponentAndStatus(Component component, VersionStatus status);

    /** Get all APPROVED versions of a component. */
    List<ComponentVersion> findByComponentIdAndStatus(Long componentId, VersionStatus status);

    /**
     * Find all APPROVED versions across the system.
     * Used by the assembly service to validate component addition.
     */
    List<ComponentVersion> findByStatus(VersionStatus status);

    /**
     * For impact analysis: find all component versions that reference a specific component.
     * Returns all versions of the given component (any status).
     */
    @Query("SELECT cv FROM ComponentVersion cv WHERE cv.component.id = :componentId ORDER BY cv.createdAt DESC")
    List<ComponentVersion> findAllByComponentIdOrderByCreatedAtDesc(Long componentId);
}
