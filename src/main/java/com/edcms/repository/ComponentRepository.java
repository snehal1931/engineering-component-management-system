package com.edcms.repository;

import com.edcms.entity.Component;
import com.edcms.enums.ComponentStatus;
import com.edcms.enums.ComponentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link Component} entities (and all its subtypes).
 *
 * <p>Because {@code Component} uses JOINED inheritance, Spring Data JPA
 * performs the necessary JOINs automatically when fetching by the base
 * type. Querying for specific subtypes (e.g., MechanicalComponent) also
 * works — Spring will restrict the query to rows with the matching discriminator.
 *
 * <p>The polymorphic queries (findAll, findByStatus) return a mix of
 * MechanicalComponent, ElectricalComponent, and SoftwareComponent instances —
 * demonstrating runtime polymorphism.
 */
@Repository
public interface ComponentRepository extends JpaRepository<Component, Long> {

    /** Look up by the globally unique component code (e.g., "ELEC-MOTOR-001"). */
    Optional<Component> findByComponentCode(String componentCode);

    /** Check if a component code already exists (for duplicate prevention). */
    boolean existsByComponentCode(String componentCode);

    /** Filter components by type — returns polymorphic list of correct subtypes. */
    List<Component> findByComponentType(ComponentType componentType);

    /** Filter components by operational status. */
    List<Component> findByStatus(ComponentStatus status);

    /** Filter by type AND status — common dashboard query. */
    List<Component> findByComponentTypeAndStatus(ComponentType componentType, ComponentStatus status);

    /** Search by name (case-insensitive partial match). */
    @Query("SELECT c FROM Component c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Component> findByNameContainingIgnoreCase(String name);
}
