package com.edcms.controller;

import com.edcms.dto.request.CreateComponentVersionRequest;
import com.edcms.dto.response.ComponentVersionResponse;
import com.edcms.service.ComponentVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Component Version lifecycle management.
 *
 * <p>Versions are always scoped under their parent component in the URL:
 * {@code /api/components/{componentId}/versions}
 *
 * <p><b>Endpoints:</b>
 * <pre>
 *   POST   /api/components/{id}/versions                   → Create a new DRAFT version
 *   GET    /api/components/{id}/versions                   → List all versions for a component
 *   GET    /api/components/{id}/versions/{versionId}       → Get specific version
 *   PATCH  /api/components/{id}/versions/{versionId}/approve    → DRAFT → APPROVED
 *   PATCH  /api/components/{id}/versions/{versionId}/deprecate  → DRAFT|APPROVED → DEPRECATED
 * </pre>
 *
 * <p><b>State machine (enforced in ComponentVersionService):</b>
 * <pre>
 *   DRAFT ──► APPROVED ──► DEPRECATED
 *     └─────────────────► DEPRECATED
 * </pre>
 */
@RestController
@RequestMapping("/api/components/{componentId}/versions")
@RequiredArgsConstructor
@Tag(name = "Component Versions", description = "Version lifecycle management — DRAFT → APPROVED → DEPRECATED")
public class ComponentVersionController {

    private final ComponentVersionService versionService;

    @PostMapping
    @Operation(
            summary = "Create a new version (DRAFT)",
            description = "New versions always start in DRAFT status. Use /approve to promote to APPROVED."
    )
    public ResponseEntity<ComponentVersionResponse> createVersion(
            @PathVariable Long componentId,
            @Valid @RequestBody CreateComponentVersionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(versionService.createVersion(componentId, request));
    }

    @GetMapping
    @Operation(summary = "List all versions for a component")
    public ResponseEntity<List<ComponentVersionResponse>> getVersionsByComponent(@PathVariable Long componentId) {
        return ResponseEntity.ok(versionService.getVersionsByComponent(componentId));
    }

    @GetMapping("/{versionId}")
    @Operation(summary = "Get a specific component version by ID")
    public ResponseEntity<ComponentVersionResponse> getVersionById(
            @PathVariable Long componentId,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(versionService.getVersionById(versionId));
    }

    /**
     * Approve a component version: DRAFT → APPROVED.
     *
     * <p>After approval, this version can be added to production assemblies.
     * Attempting to approve an already-APPROVED or DEPRECATED version will
     * return 400 Bad Request.
     */
    @PatchMapping("/{versionId}/approve")
    @Operation(
            summary = "Approve a version (DRAFT → APPROVED)",
            description = "Only APPROVED versions can be added to production assemblies. Only ADMIN should call this."
    )
    public ResponseEntity<ComponentVersionResponse> approveVersion(
            @PathVariable Long componentId,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(versionService.approveVersion(versionId));
    }

    /**
     * Deprecate a component version: DRAFT|APPROVED → DEPRECATED.
     *
     * <p>Run impact analysis first to see which assemblies will be affected:
     * {@code GET /api/impact-analysis/component-versions/{versionId}}
     */
    @PatchMapping("/{versionId}/deprecate")
    @Operation(
            summary = "Deprecate a version (DRAFT|APPROVED → DEPRECATED)",
            description = "Run /api/impact-analysis/component-versions/{id} first to see affected assemblies."
    )
    public ResponseEntity<ComponentVersionResponse> deprecateVersion(
            @PathVariable Long componentId,
            @PathVariable Long versionId) {
        return ResponseEntity.ok(versionService.deprecateVersion(versionId));
    }
}
