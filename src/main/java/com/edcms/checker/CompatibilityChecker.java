package com.edcms.checker;

import com.edcms.dto.response.CompatibilityCheckResponse;
import com.edcms.entity.ComponentVersion;

import java.util.List;

/**
 * Contract for checking compatibility between component versions.
 *
 * <p><b>Why an interface? — OOP: Dependency Inversion Principle (SOLID-D)</b><br>
 * The {@code CompatibilityService} (high-level policy) depends on THIS interface
 * (abstraction), not on {@link DefaultCompatibilityChecker} (low-level detail).
 * This means:
 * <ul>
 *   <li>The checking algorithm can be swapped without changing the service.</li>
 *   <li>The service is testable — tests can inject a mock or stub implementation.</li>
 *   <li>Future implementations (e.g., ML-based, project-type-specific) can be
 *       registered as alternative Spring beans.</li>
 * </ul>
 *
 * <p><b>Why not just a concrete class?</b><br>
 * If we used a concrete class directly, changing the algorithm would require
 * modifying {@code CompatibilityService} — violating the Open/Closed Principle.
 * With an interface, new implementations can be added without touching the service.
 *
 * <p><b>Open/Closed Principle (SOLID-O):</b><br>
 * {@code CompatibilityService} is "closed for modification" — adding a new
 * checking strategy is done by creating a new implementation of this interface,
 * not by editing the service.
 *
 * <p><b>Polymorphism:</b><br>
 * Any class implementing this interface can be injected into {@code CompatibilityService}.
 * The behavior varies by implementation, demonstrating runtime polymorphism.
 *
 * <p>Current implementation: {@link DefaultCompatibilityChecker}
 */
public interface CompatibilityChecker {

    /**
     * Check if {@code newVersion} is compatible with all {@code existingVersions}.
     *
     * <p>This is typically called before adding {@code newVersion} to an assembly
     * that already contains {@code existingVersions}.
     *
     * @param newVersion       The component version about to be added.
     * @param existingVersions All component versions already in the target assembly.
     * @return A {@link CompatibilityCheckResponse} with compatibility result and any violations.
     */
    CompatibilityCheckResponse check(ComponentVersion newVersion, List<ComponentVersion> existingVersions);

    /**
     * Check if all provided versions are mutually compatible with each other.
     *
     * <p>Checks every pair in the list — O(n²) pairs.
     * Intended for the standalone check endpoint where the user provides a set of versions.
     *
     * @param versions List of component versions to check against each other.
     * @return A {@link CompatibilityCheckResponse} with compatibility result and any violations.
     */
    CompatibilityCheckResponse checkAll(List<ComponentVersion> versions);
}
