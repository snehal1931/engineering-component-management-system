package com.edcms.repository;

import com.edcms.entity.Project;
import com.edcms.entity.User;
import com.edcms.enums.ProjectStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Project} entities.
 */
@Repository
public interface ProjectRepository extends JpaRepository<Project, Long> {

    /** Find all projects created by a specific user. */
    List<Project> findByCreatedBy(User createdBy);

    /** Filter projects by lifecycle status. */
    List<Project> findByStatus(ProjectStatus status);

    /** Check for duplicate project names (soft validation). */
    boolean existsByName(String name);

    /** Find projects by status, ordered by creation date descending. */
    List<Project> findByStatusOrderByCreatedAtDesc(ProjectStatus status);
}
