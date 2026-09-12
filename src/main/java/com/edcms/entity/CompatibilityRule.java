package com.edcms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Declares an explicit compatibility (or incompatibility) rule between two component versions.
 *
 * <p>These rules are checked by the {@link com.edcms.checker.CompatibilityChecker}
 * before adding a component version to an assembly.
 *
 * <p>Examples:
 * <ul>
 *   <li>Motor v2.0 + Controller v3.0 → {@code isCompatible = true}</li>
 *   <li>Motor v2.0 + Controller v2.0 → {@code isCompatible = false}, reason: "Requires CAN 2.0 interface"</li>
 * </ul>
 *
 * <p><b>Symmetric rule:</b><br>
 * A compatibility rule between A and B applies in both directions (A+B and B+A).
 * This means we only need one record per pair — we don't need both (A,B) and (B,A).
 * The service layer checks both orderings when querying.
 *
 * <p><b>Unique constraint:</b><br>
 * Only one rule may exist per (versionA, versionB) pair.
 * The constraint is applied on the alphabetically/numerically smaller ID first
 * to avoid duplicate entries for the same pair in different orders.
 * This is enforced at the service level.
 *
 * <p><b>Design note:</b><br>
 * The actual compatibility checking algorithm lives in the
 * {@link com.edcms.checker.CompatibilityChecker} interface, not here.
 * This entity is purely data — it stores the rules; the checker interprets them.
 * This follows the Single Responsibility Principle.
 */
@Entity
@Table(
        name = "compatibility_rules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_compatibility_rules_version_pair",
                        columnNames = {"component_version_a_id", "component_version_b_id"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompatibilityRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * First component version in the compatibility pair.
     * Always the one with the smaller database ID (enforced in the service layer).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_version_a_id", nullable = false)
    private ComponentVersion componentVersionA;

    /**
     * Second component version in the compatibility pair.
     * Always the one with the larger database ID (enforced in the service layer).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "component_version_b_id", nullable = false)
    private ComponentVersion componentVersionB;

    /**
     * Whether the two versions are compatible with each other.
     *
     * <p>{@code true} → versions can coexist in an assembly.<br>
     * {@code false} → versions cannot coexist; adding one when the other is
     * present will throw a {@link com.edcms.exception.CompatibilityException}.
     */
    @Column(name = "is_compatible", nullable = false)
    @Builder.Default
    private Boolean isCompatible = true;

    /**
     * Human-readable explanation of this compatibility rule.
     *
     * <p>Required field — engineers must document why two versions are compatible
     * or incompatible. This supports knowledge sharing and audit trails.
     *
     * <p>Example: "Motor v2.0 requires CAN 2.0 protocol supported from Controller v3.0 onwards.
     * Using with Controller v2.0 results in communication failure."
     */
    @Column(name = "rule_description", nullable = false, columnDefinition = "TEXT")
    private String ruleDescription;

    /**
     * The user who created this compatibility rule.
     * Typically an ADMIN who has verified the compatibility claim.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", referencedColumnName = "id")
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "CompatibilityRule{id=" + id + ", isCompatible=" + isCompatible + "}";
    }
}
