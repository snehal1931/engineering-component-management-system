package com.edcms.enums;

/**
 * Represents the operational status of a component (not a version).
 *
 * <p>This controls whether a component is available to be versioned
 * and referenced in assemblies. When a component is DEPRECATED,
 * no new versions should be created for it.
 */
public enum ComponentStatus {

    /** Component is actively maintained and can be versioned and assembled. */
    ACTIVE,

    /** Component has been temporarily suspended; not usable in new assemblies. */
    INACTIVE,

    /** Component is end-of-life; cannot be used in new assemblies. */
    DEPRECATED
}
