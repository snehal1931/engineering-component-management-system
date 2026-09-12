package com.edcms.entity;

import com.edcms.enums.Role;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a system user who can interact with the EDCMS application.
 *
 * <p><b>OOP Decision — Why NOT use Admin/Engineer subclasses:</b><br>
 * The requirements mention Admin and Engineer roles. However, we deliberately
 * chose a single {@code User} entity with a {@code Role} enum field rather than
 * creating {@code AdminUser extends User} and {@code EngineerUser extends User}.
 *
 * <p>Reasons:
 * <ol>
 *   <li><b>No meaningful behavioral difference</b> — Both Admins and Engineers
 *       share identical fields (username, email, etc.). The only difference is
 *       which operations they are permitted to perform, which is an authorization
 *       concern handled in the service layer, not the data model.</li>
 *   <li><b>JPA inheritance complexity</b> — Subclassing User in JPA requires a
 *       discriminator column or separate tables, adding schema complexity without
 *       any modeling benefit.</li>
 *   <li><b>Role can change</b> — A user might be promoted from ENGINEER to ADMIN.
 *       With an enum field, this is a simple update. With inheritance, it would
 *       require creating a new entity and deleting the old one.</li>
 * </ol>
 *
 * <p>Contrast this with the {@code Component} hierarchy where inheritance IS used —
 * because MechanicalComponent, ElectricalComponent, and SoftwareComponent have
 * genuinely different attributes that justify separate tables.
 *
 * <p><b>Encapsulation:</b> All fields are {@code private}. Lombok {@code @Getter}
 * and {@code @Setter} generate access methods, keeping the entity fields protected.
 */
@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
                @UniqueConstraint(name = "uk_users_email", columnNames = "email")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Unique login identifier for the user.
     * Example: "john.doe" or "jane.smith"
     */
    @Column(name = "username", nullable = false, length = 50)
    private String username;

    /**
     * Unique email address. Used for notifications and identification.
     */
    @Column(name = "email", nullable = false, length = 100)
    private String email;

    /**
     * Bcrypt-hashed password. NEVER store plain-text passwords.
     * The raw password is hashed before saving and never stored here directly.
     */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /**
     * The user's role, which controls what operations they are authorized to perform.
     * Stored as a string in the database for readability and future-safety.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    /**
     * Projects created by this user.
     *
     * <p>Mapped by the {@code createdBy} field in {@link Project}.
     * {@code FetchType.LAZY} ensures we don't load the entire project list
     * just by loading a User object — avoids unnecessary database queries.
     */
    @OneToMany(mappedBy = "createdBy", fetch = FetchType.LAZY)
    @Builder.Default
    private List<Project> projects = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Convenience method: checks if this user has the ADMIN role.
     *
     * <p>This is a business-logic convenience method on the entity.
     * Keep it simple — no authorization decisions should be made inside entities.
     */
    public boolean isAdmin() {
        return Role.ADMIN.equals(this.role);
    }

    @Override
    public String toString() {
        return "User{id=" + id + ", username='" + username + "', role=" + role + "}";
    }
}
