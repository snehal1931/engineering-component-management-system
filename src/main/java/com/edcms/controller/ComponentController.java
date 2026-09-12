package com.edcms.controller;

import com.edcms.dto.request.CreateComponentRequest;
import com.edcms.dto.response.ComponentResponse;
import com.edcms.enums.ComponentType;
import com.edcms.service.ComponentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Engineering Component management.
 *
 * <p>A single controller handles all component types (MECHANICAL, ELECTRICAL, SOFTWARE)
 * because the API is unified — clients use the {@code componentType} field to specify the type.
 *
 * <p>Base URL: {@code /api/components}
 *
 * <p><b>Endpoints:</b>
 * <pre>
 *   POST   /api/components                    → Create component (type specified in body)
 *   GET    /api/components                    → List all components
 *   GET    /api/components?type=MECHANICAL    → Filter by type
 *   GET    /api/components/{id}               → Get component by ID
 *   PATCH  /api/components/{id}/deprecate     → Deprecate a component
 * </pre>
 */
@RestController
@RequestMapping("/api/components")
@RequiredArgsConstructor
@Tag(name = "Components", description = "Engineering component catalog management (Mechanical / Electrical / Software)")
public class ComponentController {

    private final ComponentService componentService;

    @PostMapping
    @Operation(
            summary = "Create a component",
            description = "Creates a MECHANICAL, ELECTRICAL, or SOFTWARE component based on the componentType field."
    )
    public ResponseEntity<ComponentResponse> createComponent(@Valid @RequestBody CreateComponentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(componentService.createComponent(request));
    }

    /**
     * List all components, with optional filtering by type.
     *
     * <p>Example calls:
     * <pre>
     *   GET /api/components              → all types
     *   GET /api/components?type=MECHANICAL  → mechanical only
     * </pre>
     */
    @GetMapping
    @Operation(
            summary = "List all components",
            description = "Optionally filter by componentType: MECHANICAL, ELECTRICAL, SOFTWARE."
    )
    public ResponseEntity<List<ComponentResponse>> getAllComponents(
            @RequestParam(required = false) ComponentType type) {

        if (type != null) {
            return ResponseEntity.ok(componentService.getComponentsByType(type));
        }
        return ResponseEntity.ok(componentService.getAllComponents());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get component by ID")
    public ResponseEntity<ComponentResponse> getComponentById(@PathVariable Long id) {
        return ResponseEntity.ok(componentService.getComponentById(id));
    }

    /**
     * Update mutable fields of a component: name, description, manufacturer.
     *
     * <p>componentCode and componentType are immutable after creation.
     */
    @PutMapping("/{id}")
    @Operation(
            summary = "Update a component",
            description = "Updates name, description, and manufacturer. componentCode and componentType are immutable."
    )
    public ResponseEntity<ComponentResponse> updateComponent(
            @PathVariable Long id,
            @Valid @RequestBody CreateComponentRequest request) {
        return ResponseEntity.ok(componentService.updateComponent(id, request));
    }

    @PatchMapping("/{id}/deprecate")
    @Operation(
            summary = "Deprecate a component",
            description = "Marks the component as DEPRECATED. No new versions can be created after deprecation."
    )
    public ResponseEntity<ComponentResponse> deprecateComponent(@PathVariable Long id) {
        componentService.deprecateComponent(id);
        return ResponseEntity.ok(componentService.getComponentById(id));
    }
}
