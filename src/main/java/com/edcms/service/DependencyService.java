package com.edcms.service;

import com.edcms.dto.request.CreateDependencyRequest;
import com.edcms.dto.response.DependencyResponse;
import com.edcms.entity.ComponentVersion;
import com.edcms.entity.Dependency;
import com.edcms.entity.User;
import com.edcms.exception.CircularDependencyException;
import com.edcms.repository.DependencyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Service for managing component version dependencies and detecting circular dependency cycles.
 *
 * <p><b>Algorithm — Circular Dependency Detection (DFS):</b><br>
 * Before persisting any new dependency A → B, we check whether B can already
 * reach A through existing dependencies. If yes, adding A → B would create a cycle.
 *
 * <pre>
 * Example cycle to detect:
 *   Motor v2.0  →  Controller v3.0  →  Firmware v1.0  →  Motor v2.0
 *
 * When adding: Firmware v1.0 → Motor v2.0
 * DFS from Motor v2.0:
 *   Visit Motor v2.0 → Controller v3.0 → Firmware v1.0 → Motor v2.0 ← FOUND SOURCE!
 *   → CYCLE DETECTED → throw CircularDependencyException
 * </pre>
 *
 * <p><b>Time complexity:</b> O(V + E) where V = component versions, E = dependencies.
 * Acceptable for this scale. For very large graphs, topological sort algorithms
 * (like Kahn's) would be more efficient.
 *
 * <p><b>Interview note:</b> This is one of the most technically impressive parts
 * of the project — a real graph algorithm solving a genuine engineering problem.
 */
@Service
@RequiredArgsConstructor
public class DependencyService {

    private static final Logger log = LoggerFactory.getLogger(DependencyService.class);

    private final DependencyRepository dependencyRepository;
    private final ComponentVersionService versionService;
    private final UserService userService;

    @Transactional
    public DependencyResponse createDependency(CreateDependencyRequest request) {
        // Load entities
        ComponentVersion source = versionService.findVersionEntityById(request.getSourceVersionId());
        ComponentVersion target = versionService.findVersionEntityById(request.getTargetVersionId());

        // Rule 1: Self-dependency is not allowed
        if (source.getId().equals(target.getId())) {
            throw new IllegalArgumentException(
                    "A component version cannot depend on itself. Source and target must be different versions.");
        }

        // Rule 2: Duplicate dependency check
        if (dependencyRepository.existsBySourceVersionAndTargetVersion(source, target)) {
            throw new IllegalArgumentException(
                    "A dependency from '" + source.getVersionNumber() + "' to '"
                    + target.getVersionNumber() + "' already exists.");
        }

        // Rule 3: Circular dependency detection — the key algorithm
        if (wouldCreateCycle(source.getId(), target.getId())) {
            throw new CircularDependencyException(
                    "Adding '" + source.getComponent().getComponentCode() + " " + source.getVersionNumber()
                    + "' → '" + target.getComponent().getComponentCode() + " " + target.getVersionNumber()
                    + "' would create a circular dependency."
            );
        }

        User createdBy = userService.findUserEntityById(request.getCreatedById());

        Dependency dependency = Dependency.builder()
                .sourceVersion(source)
                .targetVersion(target)
                .dependencyType(request.getDependencyType())
                .description(request.getDescription())
                .createdBy(createdBy)
                .build();

        Dependency saved = dependencyRepository.save(dependency);

        log.info("Dependency created: '{}' {} → '{}'",
                source.getComponent().getComponentCode() + " " + source.getVersionNumber(),
                request.getDependencyType(),
                target.getComponent().getComponentCode() + " " + target.getVersionNumber());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<DependencyResponse> getDependenciesBySourceVersion(Long versionId) {
        ComponentVersion version = versionService.findVersionEntityById(versionId);
        return dependencyRepository.findBySourceVersion(version).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public void deleteDependency(Long dependencyId) {
        Dependency dep = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new IllegalArgumentException("Dependency not found with id: " + dependencyId));
        dependencyRepository.delete(dep);
        log.info("Dependency id: {} deleted", dependencyId);
    }

    /**
     * Checks if adding the edge (sourceId → targetId) would create a cycle.
     *
     * <p>Strategy: if we can reach {@code sourceId} starting from {@code targetId}
     * by following existing dependency edges, then adding sourceId → targetId
     * completes a cycle.
     *
     * <p>Uses DFS with a visited set to avoid infinite loops in existing cycles
     * (which shouldn't exist if this check is always applied, but defensive coding
     * is good practice).
     *
     * @param sourceId The version that would become the source of the new dependency.
     * @param targetId The version that would become the target of the new dependency.
     * @return {@code true} if the new edge would create a cycle.
     */
    boolean wouldCreateCycle(Long sourceId, Long targetId) {
        Set<Long> visited = new HashSet<>();
        return canReach(targetId, sourceId, visited);
    }

    /**
     * DFS traversal: can we reach {@code goalId} starting from {@code currentId}?
     *
     * @param currentId Start of DFS traversal.
     * @param goalId    The node we're trying to reach.
     * @param visited   Already-visited nodes (prevents infinite loops).
     */
    private boolean canReach(Long currentId, Long goalId, Set<Long> visited) {
        if (currentId.equals(goalId)) {
            return true; // Found the source — this is a cycle!
        }
        if (visited.contains(currentId)) {
            return false; // Already explored this node in this DFS path
        }

        visited.add(currentId);

        // Follow all outgoing dependency edges from currentId
        List<Dependency> outgoing = dependencyRepository.findBySourceVersionId(currentId);
        for (Dependency dep : outgoing) {
            if (canReach(dep.getTargetVersion().getId(), goalId, visited)) {
                return true;
            }
        }

        return false;
    }

    private DependencyResponse toResponse(Dependency dep) {
        ComponentVersion src = dep.getSourceVersion();
        ComponentVersion tgt = dep.getTargetVersion();

        return DependencyResponse.builder()
                .id(dep.getId())
                .dependencyType(dep.getDependencyType())
                .description(dep.getDescription())
                .createdAt(dep.getCreatedAt())
                .sourceVersionId(src.getId())
                .sourceVersionNumber(src.getVersionNumber())
                .sourceComponentCode(src.getComponent().getComponentCode())
                .sourceComponentName(src.getComponent().getName())
                .targetVersionId(tgt.getId())
                .targetVersionNumber(tgt.getVersionNumber())
                .targetComponentCode(tgt.getComponent().getComponentCode())
                .targetComponentName(tgt.getComponent().getName())
                .build();
    }
}
