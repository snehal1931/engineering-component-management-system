package com.edcms.checker;

import com.edcms.dto.response.CompatibilityCheckResponse;
import com.edcms.entity.ComponentVersion;
import com.edcms.entity.CompatibilityRule;
import com.edcms.repository.CompatibilityRuleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link CompatibilityChecker}.
 *
 * <p><b>Algorithm:</b><br>
 * For each pair of component versions, query the {@code compatibility_rules} table.
 * <ul>
 *   <li>If no rule exists for a pair → assume compatible (lenient default).</li>
 *   <li>If a rule exists with {@code isCompatible = true} → compatible.</li>
 *   <li>If a rule exists with {@code isCompatible = false} → incompatible, add to violations.</li>
 * </ul>
 *
 * <p><b>OOP — Polymorphism:</b><br>
 * This class is injected wherever a {@link CompatibilityChecker} is needed.
 * Spring's DI container resolves the concrete type at startup. If a project
 * ever needed a different strategy (e.g., a strict checker that fails when
 * NO rule exists), a new implementation would be registered with
 * {@code @Primary} or {@code @Qualifier} — without touching any service code.
 *
 * <p><b>Design decision — "No rule = compatible":</b><br>
 * We chose a lenient default: if no explicit compatibility rule exists between
 * two versions, we assume they are compatible. The alternative (strict default:
 * "no rule = incompatible") would make the system unusable until every pair is
 * explicitly declared — impractical for large component catalogs.
 *
 * <p><b>Complexity:</b> O(n²) pairs for n versions. Acceptable for assembly sizes
 * in this domain (typically &lt; 100 components). For very large assemblies,
 * caching or batch queries could be added in a future implementation.
 */
@Component
@RequiredArgsConstructor
public class DefaultCompatibilityChecker implements CompatibilityChecker {

    private static final Logger log = LoggerFactory.getLogger(DefaultCompatibilityChecker.class);

    private final CompatibilityRuleRepository compatibilityRuleRepository;

    /**
     * Check if {@code newVersion} is compatible with all versions already in the assembly.
     */
    @Override
    public CompatibilityCheckResponse check(ComponentVersion newVersion, List<ComponentVersion> existingVersions) {
        List<CompatibilityCheckResponse.IncompatiblePair> violations = new ArrayList<>();

        for (ComponentVersion existing : existingVersions) {
            // Don't check a version against itself
            if (newVersion.getId().equals(existing.getId())) continue;

            checkPair(newVersion, existing).ifPresent(violations::add);
        }

        return buildResponse(violations);
    }

    /**
     * Check all pairs in the provided list for mutual compatibility.
     */
    @Override
    public CompatibilityCheckResponse checkAll(List<ComponentVersion> versions) {
        List<CompatibilityCheckResponse.IncompatiblePair> violations = new ArrayList<>();

        // Check every unique pair (i, j) where i < j  — avoids checking (A,B) and (B,A)
        for (int i = 0; i < versions.size(); i++) {
            for (int j = i + 1; j < versions.size(); j++) {
                checkPair(versions.get(i), versions.get(j)).ifPresent(violations::add);
            }
        }

        return buildResponse(violations);
    }

    /**
     * Check a single pair of versions for a compatibility rule.
     *
     * @return An Optional containing the incompatible pair info, or empty if compatible.
     */
    private Optional<CompatibilityCheckResponse.IncompatiblePair> checkPair(
            ComponentVersion a, ComponentVersion b) {

        Optional<CompatibilityRule> rule = compatibilityRuleRepository.findByVersionPair(a, b);

        if (rule.isEmpty()) {
            // No rule defined → assume compatible (lenient default)
            log.debug("No compatibility rule found for pair ({}, {}) — assuming compatible",
                    a.getId(), b.getId());
            return Optional.empty();
        }

        CompatibilityRule foundRule = rule.get();

        if (foundRule.getIsCompatible()) {
            return Optional.empty(); // Explicit compatibility — OK
        }

        // Explicit incompatibility — report as a violation
        log.warn("Incompatibility found: '{}' vs '{}' — {}",
                describeVersion(a), describeVersion(b), foundRule.getRuleDescription());

        return Optional.of(CompatibilityCheckResponse.IncompatiblePair.builder()
                .versionAId(a.getId())
                .versionADescription(describeVersion(a))
                .versionBId(b.getId())
                .versionBDescription(describeVersion(b))
                .reason(foundRule.getRuleDescription())
                .build());
    }

    private CompatibilityCheckResponse buildResponse(
            List<CompatibilityCheckResponse.IncompatiblePair> violations) {

        boolean allCompatible = violations.isEmpty();
        String message = allCompatible
                ? "All specified component versions are compatible."
                : violations.size() + " incompatibility violation(s) found. See 'violations' for details.";

        return CompatibilityCheckResponse.builder()
                .allCompatible(allCompatible)
                .message(message)
                .violations(violations)
                .build();
    }

    /** Returns a human-readable version descriptor like "ELEC-MOTOR-001 v2.0" */
    private String describeVersion(ComponentVersion v) {
        String code = v.getComponent() != null ? v.getComponent().getComponentCode() : "UNKNOWN";
        return code + " v" + v.getVersionNumber();
    }
}
