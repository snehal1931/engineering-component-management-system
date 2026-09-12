package com.edcms.repository;

import com.edcms.entity.Assembly;
import com.edcms.entity.AssemblyComponent;
import com.edcms.entity.ComponentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link AssemblyComponent} (junction entity) records.
 *
 * <p>This repository is central to two critical features:
 * <ol>
 *   <li><b>Duplicate prevention</b>: checking if a version is already in an assembly.</li>
 *   <li><b>Impact analysis</b>: finding all assemblies that use a specific version.</li>
 * </ol>
 *
 * <p><b>JOIN FETCH on impact analysis queries:</b><br>
 * The {@code findByComponentVersionId} and {@code findByComponentId} queries use
 * {@code JOIN FETCH} to eagerly load {@code assembly}, {@code assembly.project},
 * {@code componentVersion}, and {@code componentVersion.component}. This is critical
 * for {@link com.edcms.service.ImpactAnalysisService} which accesses all these fields
 * inside the service transaction. Without JOIN FETCH, each field access would trigger
 * a separate SELECT (N+1 problem), and accessing them after session close would throw
 * {@code LazyInitializationException}.
 */
@Repository
public interface AssemblyComponentRepository extends JpaRepository<AssemblyComponent, Long> {

    /** Find all component-version memberships for a given assembly. */
    List<AssemblyComponent> findByAssembly(Assembly assembly);

    /** Find all assemblies that contain a specific component version. Used for IMPACT ANALYSIS. */
    List<AssemblyComponent> findByComponentVersion(ComponentVersion componentVersion);

    /**
     * Impact analysis: find all assemblies using a specific component version (by ID).
     *
     * <p>Uses JOIN FETCH to avoid N+1 queries and prevent LazyInitializationException
     * when {@link com.edcms.service.ImpactAnalysisService} accesses assembly, project,
     * and component details in the same transaction.
     */
    @Query("""
            SELECT ac FROM AssemblyComponent ac
            JOIN FETCH ac.assembly a
            JOIN FETCH a.project p
            JOIN FETCH ac.componentVersion cv
            JOIN FETCH cv.component c
            WHERE ac.componentVersion.id = :componentVersionId
            """)
    List<AssemblyComponent> findByComponentVersionId(Long componentVersionId);

    /**
     * Check if a specific version is already in a given assembly.
     * Used to enforce the unique constraint at the service level before DB insertion.
     */
    boolean existsByAssemblyAndComponentVersion(Assembly assembly, ComponentVersion componentVersion);

    /**
     * Find the specific membership record for (assembly, version) pair.
     * Used when removing a component version from an assembly.
     */
    Optional<AssemblyComponent> findByAssemblyAndComponentVersion(Assembly assembly, ComponentVersion componentVersion);

    /**
     * Impact analysis: find all assemblies using ANY version of a given component.
     *
     * <p>Uses JOIN FETCH to avoid N+1 queries when ImpactAnalysisService accesses
     * assembly and project details for each result.
     */
    @Query("""
            SELECT ac FROM AssemblyComponent ac
            JOIN FETCH ac.assembly a
            JOIN FETCH a.project p
            JOIN FETCH ac.componentVersion cv
            JOIN FETCH cv.component c
            WHERE cv.component.id = :componentId
            """)
    List<AssemblyComponent> findByComponentId(Long componentId);
}
