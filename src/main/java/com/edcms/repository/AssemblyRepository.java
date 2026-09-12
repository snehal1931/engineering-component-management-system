package com.edcms.repository;

import com.edcms.entity.Assembly;
import com.edcms.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for {@link Assembly} entities.
 */
@Repository
public interface AssemblyRepository extends JpaRepository<Assembly, Long> {

    /** Get all assemblies belonging to a project. */
    List<Assembly> findByProject(Project project);

    /** Get all assemblies belonging to a project by project ID. */
    List<Assembly> findByProjectId(Long projectId);

    /** Check for duplicate assembly names within the same project. */
    boolean existsByNameAndProjectId(String name, Long projectId);
}
