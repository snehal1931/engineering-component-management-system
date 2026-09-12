package com.edcms.enums;

/**
 * Represents the approval lifecycle of a component version.
 *
 * <p>This is the core state machine for component versions.
 * Valid transitions are enforced by the {@code ComponentVersionService}.
 *
 * <p>State machine:
 * <pre>
 *   DRAFT ──────► APPROVED ──────► DEPRECATED
 *     │                                 ▲
 *     └─────────────────────────────────┘
 *       (direct DRAFT→DEPRECATED is allowed to abandon a draft)
 *
 *   DEPRECATED → APPROVED : ❌ NOT ALLOWED
 *   DEPRECATED → DRAFT    : ❌ NOT ALLOWED
 *   APPROVED   → DRAFT    : ❌ NOT ALLOWED
 * </pre>
 *
 * <p>Only {@code APPROVED} versions may be added to production assemblies.
 *
 * <p>OOP note: The state machine logic could be encapsulated using the
 * State design pattern. For this project's scope, the transition rules are
 * enforced in the service layer using a clear validation method, which keeps
 * the code understandable without unnecessary complexity.
 */
public enum VersionStatus {

    /**
     * Version is in development; not yet reviewed or approved.
     * Cannot be added to assemblies.
     */
    DRAFT,

    /**
     * Version has been reviewed and approved for production use.
     * Can be added to assemblies.
     */
    APPROVED,

    /**
     * Version is no longer supported; should not be used in new assemblies.
     * Assemblies currently using this version should be flagged.
     */
    DEPRECATED
}
