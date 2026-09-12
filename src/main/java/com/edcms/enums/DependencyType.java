package com.edcms.enums;

/**
 * Classifies the type of dependency relationship between two component versions.
 *
 * <p>Example usages:
 * <ul>
 *   <li>{@code REQUIRES} — Motor v2.0 REQUIRES Controller v3.0 to function.</li>
 *   <li>{@code RECOMMENDS} — Firmware v1.5 RECOMMENDS CAN Bus Library v2.0 for performance.</li>
 *   <li>{@code CONFLICTS} — Sensor Driver v1.0 CONFLICTS with Legacy Bus Protocol v2.0.</li>
 * </ul>
 */
public enum DependencyType {

    /**
     * Hard dependency — the source version will not function correctly
     * without the target version being present in the assembly.
     */
    REQUIRES,

    /**
     * Soft dependency — the source version works without the target,
     * but performance or features are degraded.
     */
    RECOMMENDS,

    /**
     * Incompatibility declaration — the source and target versions
     * cannot be used together. This is checked during assembly composition.
     */
    CONFLICTS
}
