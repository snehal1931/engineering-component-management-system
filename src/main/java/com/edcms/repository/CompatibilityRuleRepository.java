package com.edcms.repository;

import com.edcms.entity.CompatibilityRule;
import com.edcms.entity.ComponentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link CompatibilityRule} entities.
 *
 * <p>Compatibility rules are stored symmetrically (A,B where A.id &lt; B.id).
 * The queries below check both orderings to find rules for a pair.
 */
@Repository
public interface CompatibilityRuleRepository extends JpaRepository<CompatibilityRule, Long> {

    /**
     * Find the compatibility rule between two specific versions.
     * Checks both orderings because the rule is stored with the smaller ID first.
     *
     * <p>This is the primary query used by {@code DefaultCompatibilityChecker}.
     */
    @Query("""
            SELECT cr FROM CompatibilityRule cr
            WHERE (cr.componentVersionA = :versionA AND cr.componentVersionB = :versionB)
               OR (cr.componentVersionA = :versionB AND cr.componentVersionB = :versionA)
            """)
    Optional<CompatibilityRule> findByVersionPair(
            ComponentVersion versionA,
            ComponentVersion versionB
    );

    /**
     * Find all compatibility rules involving a specific component version.
     * Used when displaying what rules apply to a given version.
     */
    @Query("""
            SELECT cr FROM CompatibilityRule cr
            WHERE cr.componentVersionA = :version OR cr.componentVersionB = :version
            """)
    List<CompatibilityRule> findAllByVersion(ComponentVersion version);

    /**
     * Check if a rule already exists for this pair (to prevent duplicates).
     */
    @Query("""
            SELECT COUNT(cr) > 0 FROM CompatibilityRule cr
            WHERE (cr.componentVersionA.id = :idA AND cr.componentVersionB.id = :idB)
               OR (cr.componentVersionA.id = :idB AND cr.componentVersionB.id = :idA)
            """)
    boolean existsByVersionPair(Long idA, Long idB);
}
