package com.edcms.controller;

import com.edcms.dto.request.ChangeProjectStatusRequest;
import com.edcms.dto.request.CreateProjectRequest;
import com.edcms.dto.response.ProjectResponse;
import com.edcms.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for engineering project management.
 *
 * <p>Base URL: {@code /api/projects}
 *
 * <p><b>Endpoints:</b>
 * <pre>
 *   POST   /api/projects                  → Create project
 *   GET    /api/projects                  → List all projects
 *   GET    /api/projects/{id}             → Get project by ID
 *   PUT    /api/projects/{id}             → Update project name/description
 *   PATCH  /api/projects/{id}/status      → Change project status (state machine)
 *   DELETE /api/projects/{id}             → Delete project (cascades to assemblies)
 * </pre>
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
@Tag(name = "Projects", description = "Engineering project management")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping
    @Operation(summary = "Create a new engineering project")
    public ResponseEntity<ProjectResponse> createProject(@Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(projectService.createProject(request));
    }

    @GetMapping
    @Operation(summary = "Get all projects")
    public ResponseEntity<List<ProjectResponse>> getAllProjects() {
        return ResponseEntity.ok(projectService.getAllProjects());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get project by ID")
    public ResponseEntity<ProjectResponse> getProjectById(@PathVariable Long id) {
        return ResponseEntity.ok(projectService.getProjectById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update project name and description")
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long id,
            @Valid @RequestBody CreateProjectRequest request) {
        return ResponseEntity.ok(projectService.updateProject(id, request));
    }

    /**
     * Change the project lifecycle status.
     *
     * <p>Valid transitions:
     * <pre>
     *   PLANNING → ACTIVE → COMPLETED → ARCHIVED
     *             └──────────────────→ ARCHIVED
     * </pre>
     * Invalid transitions return 400 Bad Request.
     */
    @PatchMapping("/{id}/status")
    @Operation(
            summary = "Change project status",
            description = "Valid transitions: PLANNING→ACTIVE, ACTIVE→COMPLETED, ACTIVE→ARCHIVED, COMPLETED→ARCHIVED."
    )
    public ResponseEntity<ProjectResponse> changeStatus(
            @PathVariable Long id,
            @Valid @RequestBody ChangeProjectStatusRequest request) {
        return ResponseEntity.ok(projectService.changeStatus(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a project (cascades to assemblies)")
    public ResponseEntity<Void> deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
        return ResponseEntity.noContent().build();
    }
}
