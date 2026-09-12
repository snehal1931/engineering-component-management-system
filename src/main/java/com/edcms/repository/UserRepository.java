package com.edcms.repository;

import com.edcms.entity.User;
import com.edcms.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link User} entities.
 *
 * <p>Spring Data JPA generates the SQL for all these methods at startup.
 * No implementation code is required — method names follow Spring's
 * query derivation convention.
 *
 * <p>OOP: The {@link JpaRepository} interface provides full CRUD operations
 * via the Template Method pattern. We extend it and add domain-specific
 * query methods that Spring implements automatically.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /** Used during login / authentication lookup. */
    Optional<User> findByUsername(String username);

    /** Used to check for duplicate email addresses during registration. */
    Optional<User> findByEmail(String email);

    /** Used to check for duplicate usernames during registration. */
    boolean existsByUsername(String username);

    /** Used to check for duplicate emails during registration. */
    boolean existsByEmail(String email);

    /** Find all users with a specific role — useful for admin dashboards. */
    List<User> findByRole(Role role);
}
