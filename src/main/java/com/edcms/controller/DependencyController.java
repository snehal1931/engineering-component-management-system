package com.edcms.controller;

import com.edcms.dto.request.CreateDependencyRequest;
import com.edcms.dto.response.DependencyResponse;
import com.edcms.service.DependencyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for component version dependency management.
 *
 * <p>Models the directed dependency graph between component versions.
 * Adding a dependency triggers DFS-based circular dependency detection.
 *
 * <p><b>Endpoints:</b>
 * <pre>
 *   POST   /api/dependencies                               → Declare dependency (with cycle check)
 *   GET    /api/component-versions/{id}/dependencies       → Get all dependencies of a version
 *   DELETE /api/dependencies/{id}                          → Remove a dependency declaration
 * </pre>
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Dependencies", description = "Component version dependency graph management with circular dependency detection")
public class DependencyController {

    private final DependencyService dependencyService;

    @PostMapping("/api/dependencies")
    @Operation(
            summary = "Declare a dependency between two component versions",
            description = "Enforces: no self-dependency, no duplicates, no circular dependencies (DFS detection)."
    )
    public ResponseEntity<DependencyResponse> createDependency(@Valid @RequestBody CreateDependencyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(dependencyService.createDependency(request));
    }

    @GetMapping("/api/component-versions/{versionId}/dependencies")
    @Operation(
            summary = "Get all dependencies declared by a component version",
            description = "Returns outgoing dependencies — things this version REQUIRES, RECOMMENDS, or CONFLICTS with."
    )
    public ResponseEntity<List<DependencyResponse>> getDependencies(@PathVariable Long versionId) {
        return ResponseEntity.ok(dependencyService.getDependenciesBySourceVersion(versionId));
    }

    @DeleteMapping("/api/dependencies/{id}")
    @Operation(summary = "Delete a dependency declaration")
    public ResponseEntity<Void> deleteDependency(@PathVariable Long id) {
        dependencyService.deleteDependency(id);
        return ResponseEntity.noContent().build();
    }
}
