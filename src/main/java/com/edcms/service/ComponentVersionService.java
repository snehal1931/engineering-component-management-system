package com.edcms.service;

import com.edcms.dto.request.CreateComponentVersionRequest;
import com.edcms.dto.response.ComponentVersionResponse;
import com.edcms.entity.Component;
import com.edcms.entity.ComponentVersion;
import com.edcms.entity.User;
import com.edcms.enums.VersionStatus;
import com.edcms.exception.ComponentVersionNotFoundException;
import com.edcms.exception.InvalidVersionTransitionException;
import com.edcms.mapper.ComponentVersionMapper;
import com.edcms.repository.ComponentVersionRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service for Component Version lifecycle management.
 *
 * <p>This service is the most business-logic-heavy in the system.
 * It enforces the version state machine, prevents duplicate versions,
 * and ensures only APPROVED versions are approved.
 *
 * <p><b>Version State Machine (enforced here):</b>
 * <pre>
 *   DRAFT ──────────────► APPROVED ──► DEPRECATED
 *     └──────────────────────────────► DEPRECATED
 *
 *   Forbidden:
 *     DEPRECATED → APPROVED  ❌
 *     DEPRECATED → DRAFT     ❌
 *     APPROVED   → DRAFT     ❌
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ComponentVersionService {

    private static final Logger log = LoggerFactory.getLogger(ComponentVersionService.class);

    private final ComponentVersionRepository versionRepository;
    private final ComponentService componentService;
    private final UserService userService;
    private final ComponentVersionMapper versionMapper;

    /**
     * Create a new version for a component.
     * New versions always start in DRAFT status.
     */
    @Transactional
    public ComponentVersionResponse createVersion(Long componentId, CreateComponentVersionRequest request) {
        Component component = componentService.findComponentEntityById(componentId);

        // Prevent duplicate version numbers per component
        if (versionRepository.existsByComponentAndVersionNumber(component, request.getVersionNumber())) {
            throw new IllegalArgumentException(
                    "Version '" + request.getVersionNumber() + "' already exists for component '"
                    + component.getComponentCode() + "'. Version numbers must be unique per component."
            );
        }

        User createdBy = userService.findUserEntityById(request.getCreatedById());

        ComponentVersion version = ComponentVersion.builder()
                .component(component)
                .versionNumber(request.getVersionNumber())
                .releaseNotes(request.getReleaseNotes())
                .status(VersionStatus.DRAFT) // Always starts as DRAFT
                .createdBy(createdBy)
                .build();

        ComponentVersion saved = versionRepository.save(version);
        log.info("Version '{}' created for component '{}' (id: {})",
                saved.getVersionNumber(), component.getComponentCode(), saved.getId());
        return versionMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ComponentVersionResponse> getVersionsByComponent(Long componentId) {
        // Verify the component exists first
        componentService.findComponentEntityById(componentId);
        return versionRepository.findByComponentId(componentId).stream()
                .map(versionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComponentVersionResponse getVersionById(Long versionId) {
        return versionMapper.toResponse(findVersionEntityById(versionId));
    }

    /**
     * Approve a component version: DRAFT → APPROVED.
     *
     * <p>After approval, this version can be added to production assemblies.
     * Only ADMIN users should call this endpoint (enforced at controller level).
     */
    @Transactional
    public ComponentVersionResponse approveVersion(Long versionId) {
        ComponentVersion version = findVersionEntityById(versionId);
        validateTransition(version.getStatus(), VersionStatus.APPROVED);

        version.setStatus(VersionStatus.APPROVED);
        ComponentVersion saved = versionRepository.save(version);

        log.info("Version '{}' (id: {}) APPROVED for component '{}'",
                saved.getVersionNumber(), saved.getId(),
                saved.getComponent().getComponentCode());
        return versionMapper.toResponse(saved);
    }

    /**
     * Deprecate a component version: DRAFT|APPROVED → DEPRECATED.
     *
     * <p>After deprecation, assemblies using this version should be reviewed.
     * Impact analysis should be run before deprecating a widely-used version.
     */
    @Transactional
    public ComponentVersionResponse deprecateVersion(Long versionId) {
        ComponentVersion version = findVersionEntityById(versionId);
        validateTransition(version.getStatus(), VersionStatus.DEPRECATED);

        version.setStatus(VersionStatus.DEPRECATED);
        ComponentVersion saved = versionRepository.save(version);

        log.warn("Version '{}' (id: {}) DEPRECATED for component '{}'. Run impact analysis to find affected assemblies.",
                saved.getVersionNumber(), saved.getId(),
                saved.getComponent().getComponentCode());
        return versionMapper.toResponse(saved);
    }

    /**
     * Internal entity loader — used by AssemblyService, DependencyService, CompatibilityService.
     */
    @Transactional(readOnly = true)
    public ComponentVersion findVersionEntityById(Long id) {
        return versionRepository.findById(id)
                .orElseThrow(() -> new ComponentVersionNotFoundException(id));
    }

    /**
     * Enforces the version state machine transition rules.
     *
     * <p>This is the core business logic method for versioning.
     * Extracted into its own method so it can be unit-tested independently
     * and easily explained in an interview.
     *
     * <p><b>Why not the State design pattern?</b><br>
     * The State pattern would create a State interface and concrete classes
     * (DraftState, ApprovedState, DeprecatedState) each with transition methods.
     * For three states with simple rules, this is over-engineered — the added
     * abstraction obscures rather than clarifies the intent.
     * A simple switch expression is more readable and equally maintainable here.
     *
     * @throws InvalidVersionTransitionException if the transition is forbidden.
     */
    public void validateTransition(VersionStatus current, VersionStatus next) {
        Set<VersionStatus> allowed = switch (current) {
            case DRAFT      -> Set.of(VersionStatus.APPROVED, VersionStatus.DEPRECATED);
            case APPROVED   -> Set.of(VersionStatus.DEPRECATED);
            case DEPRECATED -> Set.of(); // Terminal state — no transitions out
        };

        if (!allowed.contains(next)) {
            throw new InvalidVersionTransitionException(current.name(), next.name());
        }
    }
}
