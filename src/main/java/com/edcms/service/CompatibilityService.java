package com.edcms.service;

import com.edcms.checker.CompatibilityChecker;
import com.edcms.dto.request.CompatibilityCheckRequest;
import com.edcms.dto.request.CreateCompatibilityRuleRequest;
import com.edcms.dto.response.CompatibilityCheckResponse;
import com.edcms.dto.response.CompatibilityRuleResponse;
import com.edcms.entity.CompatibilityRule;
import com.edcms.entity.ComponentVersion;
import com.edcms.entity.User;
import com.edcms.exception.CompatibilityException;
import com.edcms.repository.CompatibilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for compatibility rule management and compatibility checking.
 *
 * <p><b>Dependency Inversion in practice:</b><br>
 * This service depends on the {@link CompatibilityChecker} INTERFACE,
 * not on {@code DefaultCompatibilityChecker} directly. Spring injects
 * the concrete bean at runtime. This means:
 * <ul>
 *   <li>In production: {@code DefaultCompatibilityChecker} is injected.</li>
 *   <li>In unit tests: a mock is injected — no database needed.</li>
 *   <li>In future: a different implementation can be registered without
 *       changing this service code.</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class CompatibilityService {

    private static final Logger log = LoggerFactory.getLogger(CompatibilityService.class);

    private final CompatibilityRuleRepository compatibilityRuleRepository;
    private final CompatibilityChecker compatibilityChecker;   // ← injected interface, not concrete class
    private final ComponentVersionService versionService;
    private final UserService userService;

    /**
     * Create a new compatibility rule between two component versions.
     *
     * <p>Rules are stored symmetrically — the version with the smaller ID
     * is always stored as versionA to prevent duplicate (A,B) and (B,A) entries.
     */
    @Transactional
    public CompatibilityRuleResponse createRule(CreateCompatibilityRuleRequest request) {
        ComponentVersion vA = versionService.findVersionEntityById(request.getVersionAId());
        ComponentVersion vB = versionService.findVersionEntityById(request.getVersionBId());

        if (vA.getId().equals(vB.getId())) {
            throw new IllegalArgumentException("Cannot create a compatibility rule between a version and itself.");
        }

        if (compatibilityRuleRepository.existsByVersionPair(vA.getId(), vB.getId())) {
            throw new CompatibilityException(
                    "A compatibility rule already exists between version "
                    + vA.getVersionNumber() + " and " + vB.getVersionNumber() + ".");
        }

        // Normalize storage order: smaller ID goes into versionA
        ComponentVersion storedA = vA.getId() < vB.getId() ? vA : vB;
        ComponentVersion storedB = vA.getId() < vB.getId() ? vB : vA;

        User createdBy = userService.findUserEntityById(request.getCreatedById());

        CompatibilityRule rule = CompatibilityRule.builder()
                .componentVersionA(storedA)
                .componentVersionB(storedB)
                .isCompatible(request.getIsCompatible())
                .ruleDescription(request.getRuleDescription())
                .createdBy(createdBy)
                .build();

        CompatibilityRule saved = compatibilityRuleRepository.save(rule);
        log.info("Compatibility rule created: {} {} {} — compatible: {}",
                storedA.getComponent().getComponentCode(), storedA.getVersionNumber(),
                storedB.getVersionNumber(), request.getIsCompatible());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CompatibilityRuleResponse> getAllRules() {
        return compatibilityRuleRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Check mutual compatibility of a provided list of component version IDs.
     * Used by the standalone {@code POST /api/compatibility/check} endpoint.
     */
    @Transactional(readOnly = true)
    public CompatibilityCheckResponse checkVersions(CompatibilityCheckRequest request) {
        List<ComponentVersion> versions = request.getComponentVersionIds().stream()
                .map(versionService::findVersionEntityById)
                .toList();

        log.info("Running compatibility check on {} versions", versions.size());
        return compatibilityChecker.checkAll(versions);
    }

    /**
     * Check if a new version is compatible with all existing versions in an assembly.
     * Called by {@code AssemblyController} before adding a version.
     *
     * @throws CompatibilityException if any incompatibility is found.
     */
    @Transactional(readOnly = true)
    public void validateBeforeAdding(ComponentVersion newVersion, List<ComponentVersion> existingVersions) {
        if (existingVersions.isEmpty()) return; // Nothing to check

        CompatibilityCheckResponse result = compatibilityChecker.check(newVersion, existingVersions);

        if (!result.isAllCompatible()) {
            String violations = result.getViolations().stream()
                    .map(v -> v.getVersionADescription() + " ✗ " + v.getVersionBDescription()
                              + ": " + v.getReason())
                    .reduce((a, b) -> a + "; " + b)
                    .orElse("Unknown incompatibility");

            throw new CompatibilityException("Cannot add version — compatibility violation(s) found: " + violations);
        }
    }

    private CompatibilityRuleResponse toResponse(CompatibilityRule rule) {
        ComponentVersion a = rule.getComponentVersionA();
        ComponentVersion b = rule.getComponentVersionB();

        return CompatibilityRuleResponse.builder()
                .id(rule.getId())
                .compatible(rule.getIsCompatible())
                .ruleDescription(rule.getRuleDescription())
                .createdAt(rule.getCreatedAt())
                .versionAId(a.getId())
                .versionANumber(a.getVersionNumber())
                .versionAComponentCode(a.getComponent().getComponentCode())
                .versionBId(b.getId())
                .versionBNumber(b.getVersionNumber())
                .versionBComponentCode(b.getComponent().getComponentCode())
                .build();
    }
}
