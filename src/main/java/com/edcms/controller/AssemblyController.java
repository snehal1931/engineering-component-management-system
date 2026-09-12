package com.edcms.controller;

import com.edcms.dto.request.AddComponentToAssemblyRequest;
import com.edcms.dto.request.CreateAssemblyRequest;
import com.edcms.dto.response.AssemblyResponse;
import com.edcms.service.AssemblyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Assembly management (Bill of Materials composition).
 *
 * <p><b>Endpoints:</b>
 * <pre>
 *   POST   /api/projects/{projectId}/assemblies          → Create assembly in project
 *   GET    /api/projects/{projectId}/assemblies          → List assemblies in project
 *   GET    /api/assemblies/{id}                          → Get assembly by ID
 *   POST   /api/assemblies/{id}/components               → Add component version to assembly
 *   DELETE /api/assemblies/{id}/components/{versionId}   → Remove component version
 *   DELETE /api/assemblies/{id}                          → Delete assembly
 * </pre>
 *
 * <p><b>Design note:</b><br>
 * All business rules (version status check, duplicate check, compatibility check) are
 * enforced inside {@link AssemblyService#addComponentVersion} within a single
 * {@code @Transactional} boundary. The controller is intentionally thin — it only
 * handles HTTP concerns (request parsing, response status codes).
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Assemblies", description = "Assembly (Bill of Materials) composition and management")
public class AssemblyController {

    private final AssemblyService assemblyService;

    @PostMapping("/api/projects/{projectId}/assemblies")
    @Operation(summary = "Create a new assembly within a project")
    public ResponseEntity<AssemblyResponse> createAssembly(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateAssemblyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(assemblyService.createAssembly(projectId, request));
    }

    @GetMapping("/api/projects/{projectId}/assemblies")
    @Operation(summary = "List all assemblies in a project")
    public ResponseEntity<List<AssemblyResponse>> getAssembliesByProject(@PathVariable Long projectId) {
        return ResponseEntity.ok(assemblyService.getAssembliesByProject(projectId));
    }

    @GetMapping("/api/assemblies/{id}")
    @Operation(summary = "Get a specific assembly with its full Bill of Materials")
    public ResponseEntity<AssemblyResponse> getAssemblyById(@PathVariable Long id) {
        return ResponseEntity.ok(assemblyService.getAssemblyById(id));
    }

    /**
     * Add a component version to an assembly.
     *
     * <p>All three business rules are enforced inside {@link AssemblyService#addComponentVersion}:
     * <ol>
     *   <li>Version must be APPROVED.</li>
     *   <li>No duplicate in same assembly.</li>
     *   <li>Compatible with all existing assembly versions.</li>
     * </ol>
     */
    @PostMapping("/api/assemblies/{id}/components")
    @Operation(
            summary = "Add a component version to an assembly",
            description = "Enforces: version must be APPROVED, no duplicates, compatible with existing assembly versions."
    )
    public ResponseEntity<AssemblyResponse> addComponentVersion(
            @PathVariable Long id,
            @Valid @RequestBody AddComponentToAssemblyRequest request) {
        return ResponseEntity.ok(assemblyService.addComponentVersion(id, request));
    }

    @DeleteMapping("/api/assemblies/{id}/components/{componentVersionId}")
    @Operation(summary = "Remove a component version from an assembly")
    public ResponseEntity<Void> removeComponentVersion(
            @PathVariable Long id,
            @PathVariable Long componentVersionId) {
        assemblyService.removeComponentVersion(id, componentVersionId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/assemblies/{id}")
    @Operation(summary = "Delete an assembly and all its component memberships")
    public ResponseEntity<Void> deleteAssembly(@PathVariable Long id) {
        assemblyService.deleteAssembly(id);
        return ResponseEntity.noContent().build();
    }
}
