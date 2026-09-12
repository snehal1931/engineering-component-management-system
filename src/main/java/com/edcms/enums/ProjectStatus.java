package com.edcms.enums;

/**
 * Represents the lifecycle status of an engineering project.
 *
 * <p>Valid status flow:
 * <pre>
 *   PLANNING → ACTIVE → COMPLETED → ARCHIVED
 * </pre>
 * Status transitions are enforced in the ProjectService to prevent
 * illogical state changes (e.g., ARCHIVED → PLANNING).
 */
public enum ProjectStatus {

    /** Project is being scoped; no active development yet. */
    PLANNING,

    /** Project is actively being developed and components are being assembled. */
    ACTIVE,

    /** Project deliverables are finalized. */
    COMPLETED,

    /** Project is read-only; no further modifications expected. */
    ARCHIVED
}
