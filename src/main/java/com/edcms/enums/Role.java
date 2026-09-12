package com.edcms.enums;

/**
 * Defines the roles a user can have in the system.
 *
 * <p>Used for role-based access control. Rather than subclassing User into
 * AdminUser and EngineerUser (which adds awkward JPA complexity with little
 * behavioral benefit), we store role as an enum field on the User entity and
 * enforce role-based rules in the service layer.
 *
 * <p>OOP note: This is a deliberate choice NOT to use inheritance here.
 * Inheritance should be used for "is-a" relationships with meaningful behavioral
 * differences, not just for data tagging. A role is a tag, not a type.
 */
public enum Role {

    /**
     * Full system administrator.
     * Can approve/deprecate component versions, manage users, and manage compatibility rules.
     */
    ADMIN,

    /**
     * Engineering user.
     * Can create projects, components, assemblies, and run analysis.
     */
    ENGINEER
}
