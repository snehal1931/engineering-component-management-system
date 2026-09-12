package com.edcms.service;

import com.edcms.dto.request.ChangeProjectStatusRequest;
import com.edcms.dto.request.CreateProjectRequest;
import com.edcms.dto.response.ProjectResponse;
import com.edcms.entity.Project;
import com.edcms.entity.User;
import com.edcms.enums.ProjectStatus;
import com.edcms.exception.ProjectNotFoundException;
import com.edcms.mapper.ProjectMapper;
import com.edcms.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Service for Engineering Project management operations.
 *
 * <p>Enforces the project lifecycle status machine:
 * <pre>
 *   PLANNING → ACTIVE → COMPLETED → ARCHIVED
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);

    private final ProjectRepository projectRepository;
    private final UserService userService;
    private final ProjectMapper projectMapper;

    @Transactional
    public ProjectResponse createProject(CreateProjectRequest request) {
        log.info("Creating project: '{}'", request.getName());

        User createdBy = userService.findUserEntityById(request.getCreatedById());

        Project project = Project.builder()
                .name(request.getName())
                .description(request.getDescription())
                .status(ProjectStatus.PLANNING)
                .createdBy(createdBy)
                .build();

        Project saved = projectRepository.save(project);
        log.info("Project '{}' created with id: {}", saved.getName(), saved.getId());
        return projectMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectResponse> getAllProjects() {
        return projectRepository.findAll().stream()
                .map(projectMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjectResponse getProjectById(Long id) {
        return projectMapper.toResponse(findProjectEntityById(id));
    }

    @Transactional
    public ProjectResponse updateProject(Long id, CreateProjectRequest request) {
        Project project = findProjectEntityById(id);
        project.setName(request.getName());
        project.setDescription(request.getDescription());
        Project saved = projectRepository.save(project);
        log.info("Project id: {} updated", id);
        return projectMapper.toResponse(saved);
    }

    /**
     * Change the lifecycle status of a project.
     *
     * <p>Enforces valid status transitions.
     * Invalid transitions throw an {@link IllegalArgumentException}
     * (caught by GlobalExceptionHandler as 400 Bad Request).
     */
    @Transactional
    public ProjectResponse changeStatus(Long id, ChangeProjectStatusRequest request) {
        Project project = findProjectEntityById(id);
        ProjectStatus current = project.getStatus();
        ProjectStatus next = request.getStatus();

        validateProjectStatusTransition(current, next);

        project.setStatus(next);
        Project saved = projectRepository.save(project);
        log.info("Project id: {} status changed: {} → {}", id, current, next);
        return projectMapper.toResponse(saved);
    }

    @Transactional
    public void deleteProject(Long id) {
        Project project = findProjectEntityById(id);
        log.warn("Deleting project id: {} ('{}'), cascading to {} assemblies",
                id, project.getName(), project.getAssemblies().size());
        projectRepository.delete(project);
    }

    /**
     * Internal entity loader for other services that need to bind a Project
     * as a JPA relationship (e.g., AssemblyService creating an Assembly).
     */
    @Transactional(readOnly = true)
    public Project findProjectEntityById(Long id) {
        return projectRepository.findById(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
    }

    /**
     * Validates that a project status transition is logically allowed.
     *
     * <p>Allowed transitions:
     * <pre>
     *   PLANNING → ACTIVE
     *   ACTIVE   → COMPLETED
     *   ACTIVE   → ARCHIVED
     *   COMPLETED → ARCHIVED
     * </pre>
     */
    private void validateProjectStatusTransition(ProjectStatus current, ProjectStatus next) {
        Set<ProjectStatus> allowedNext = switch (current) {
            case PLANNING  -> Set.of(ProjectStatus.ACTIVE);
            case ACTIVE    -> Set.of(ProjectStatus.COMPLETED, ProjectStatus.ARCHIVED);
            case COMPLETED -> Set.of(ProjectStatus.ARCHIVED);
            case ARCHIVED  -> Set.of(); // No transitions from ARCHIVED
        };

        if (!allowedNext.contains(next)) {
            throw new IllegalArgumentException(
                    "Invalid project status transition: " + current + " → " + next
                    + ". Allowed next statuses: " + allowedNext
            );
        }
    }
}
