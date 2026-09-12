package com.edcms.service;

import com.edcms.dto.request.AddComponentToAssemblyRequest;
import com.edcms.dto.request.CreateAssemblyRequest;
import com.edcms.dto.response.AssemblyResponse;
import com.edcms.entity.Assembly;
import com.edcms.entity.AssemblyComponent;
import com.edcms.entity.ComponentVersion;
import com.edcms.entity.Project;
import com.edcms.entity.User;
import com.edcms.enums.VersionStatus;
import com.edcms.exception.AssemblyNotFoundException;
import com.edcms.exception.CompatibilityException;
import com.edcms.exception.InvalidVersionStatusException;
import com.edcms.mapper.AssemblyMapper;
import com.edcms.repository.AssemblyComponentRepository;
import com.edcms.repository.AssemblyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Assembly management and Bill of Materials (BoM) composition.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li>Create assemblies within projects.</li>
 *   <li>Add/remove component versions from assemblies.</li>
 *   <li>Enforce business rules: only APPROVED versions, no duplicates, compatibility checks.</li>
 * </ul>
 *
 * <p><b>Composition in practice:</b><br>
 * This service orchestrates the composition of {@link Assembly} objects from
 * {@link ComponentVersion} instances. It doesn't use inheritance — it uses
 * composition to build complex structures from simpler ones.
 */
@Service
@RequiredArgsConstructor
public class AssemblyService {

    private static final Logger log = LoggerFactory.getLogger(AssemblyService.class);

    private final AssemblyRepository assemblyRepository;
    private final AssemblyComponentRepository assemblyComponentRepository;
    private final ProjectService projectService;
    private final ComponentVersionService versionService;
    private final UserService userService;
    private final AssemblyMapper assemblyMapper;
    private final CompatibilityService compatibilityService;

    @Transactional
    public AssemblyResponse createAssembly(Long projectId, CreateAssemblyRequest request) {
        log.info("Creating assembly '{}' in project id: {}", request.getName(), projectId);

        Project project = projectService.findProjectEntityById(projectId);
        User createdBy = userService.findUserEntityById(request.getCreatedById());

        Assembly assembly = Assembly.builder()
                .name(request.getName())
                .description(request.getDescription())
                .project(project)
                .createdBy(createdBy)
                .build();

        Assembly saved = assemblyRepository.save(assembly);
        log.info("Assembly '{}' (id: {}) created in project '{}'",
                saved.getName(), saved.getId(), project.getName());
        return assemblyMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<AssemblyResponse> getAssembliesByProject(Long projectId) {
        // Verify project exists
        projectService.findProjectEntityById(projectId);
        return assemblyRepository.findByProjectId(projectId).stream()
                .map(assemblyMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AssemblyResponse getAssemblyById(Long id) {
        return assemblyMapper.toResponse(findAssemblyEntityById(id));
    }

    /**
     * Add a specific component version to an assembly.
     *
     * <p>Enforces three critical business rules in sequence — all within a single
     * {@code @Transactional} boundary to prevent LazyInitializationException:
     * <ol>
     *   <li><b>Only APPROVED versions allowed.</b> DRAFT or DEPRECATED versions
     *       cannot be added to production assemblies.</li>
     *   <li><b>No duplicates.</b> The same version cannot be added to the same
     *       assembly twice (enforced at both service and DB level).</li>
     *   <li><b>Compatibility check.</b> The new version must be compatible with
     *       all versions already in the assembly (via CompatibilityRule records).</li>
     * </ol>
     *
     * <p><b>Bug fix note:</b> The compatibility check was previously performed in the
     * controller by accessing {@code assembly.getAssemblyComponents()} — a LAZY collection —
     * outside a {@code @Transactional} boundary, causing {@code LazyInitializationException}.
     * It is now correctly performed here, inside the transaction.
     */
    @Transactional
    public AssemblyResponse addComponentVersion(Long assemblyId, AddComponentToAssemblyRequest request) {
        Assembly assembly = findAssemblyEntityById(assemblyId);
        ComponentVersion version = versionService.findVersionEntityById(request.getComponentVersionId());

        // Rule 1: Only APPROVED versions can be added
        if (!VersionStatus.APPROVED.equals(version.getStatus())) {
            throw new InvalidVersionStatusException(version.getVersionNumber(), version.getStatus().name());
        }

        // Rule 2: No duplicate versions in the same assembly
        if (assemblyComponentRepository.existsByAssemblyAndComponentVersion(assembly, version)) {
            throw new CompatibilityException(
                    "Version '" + version.getVersionNumber() + "' of component '"
                    + version.getComponent().getComponentCode()
                    + "' is already present in assembly '" + assembly.getName() + "'."
            );
        }

        // Rule 3: Compatibility check — must be inside the transaction to safely
        // access the LAZY assemblyComponents collection.
        List<ComponentVersion> existingVersions = assembly.getAssemblyComponents().stream()
                .map(AssemblyComponent::getComponentVersion)
                .toList();
        compatibilityService.validateBeforeAdding(version, existingVersions);

        AssemblyComponent assemblyComponent = AssemblyComponent.builder()
                .assembly(assembly)
                .componentVersion(version)
                .quantity(request.getQuantity())
                .notes(request.getNotes())
                .build();

        assemblyComponentRepository.save(assemblyComponent);

        // Add to in-memory collection to avoid stale first-level cache issue.
        // The assemblyRepository.findById() within the same transaction would return
        // the same cached Assembly entity with the old (un-updated) assemblyComponents list.
        // Adding the new component to the in-memory list ensures the response reflects it.
        assembly.getAssemblyComponents().add(assemblyComponent);

        log.info("Version '{}' (id: {}) added to assembly '{}' (id: {})",
                version.getVersionNumber(), version.getId(),
                assembly.getName(), assembly.getId());

        return assemblyMapper.toResponse(assembly);

    }

    /**
     * Remove a component version from an assembly.
     */
    @Transactional
    public void removeComponentVersion(Long assemblyId, Long componentVersionId) {
        Assembly assembly = findAssemblyEntityById(assemblyId);
        ComponentVersion version = versionService.findVersionEntityById(componentVersionId);

        AssemblyComponent ac = assemblyComponentRepository
                .findByAssemblyAndComponentVersion(assembly, version)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Version '" + version.getVersionNumber() + "' is not part of assembly '"
                        + assembly.getName() + "'."
                ));

        assemblyComponentRepository.delete(ac);
        log.info("Version '{}' removed from assembly '{}'", version.getVersionNumber(), assembly.getName());
    }

    @Transactional
    public void deleteAssembly(Long id) {
        Assembly assembly = findAssemblyEntityById(id);
        log.warn("Deleting assembly id: {} ('{}')", id, assembly.getName());
        assemblyRepository.delete(assembly);
    }

    /** Internal entity loader for other services. */
    @Transactional(readOnly = true)
    public Assembly findAssemblyEntityById(Long id) {
        return assemblyRepository.findById(id)
                .orElseThrow(() -> new AssemblyNotFoundException(id));
    }
}
