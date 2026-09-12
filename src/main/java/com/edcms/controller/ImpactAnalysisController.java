package com.edcms.controller;

import com.edcms.dto.response.ImpactAnalysisResponse;
import com.edcms.service.ImpactAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for impact analysis.
 *
 * <p>Answers the critical engineering question:
 * <em>"If I deprecate this component version, what assemblies are affected?"</em>
 *
 * <p><b>Endpoints:</b>
 * <pre>
 *   GET /api/impact-analysis/component-versions/{id}   → Which assemblies use this specific version?
 *   GET /api/impact-analysis/components/{id}            → Which assemblies use ANY version of this component?
 * </pre>
 *
 * <p><b>Recommended workflow before deprecating a version:</b>
 * <ol>
 *   <li>Call {@code GET /api/impact-analysis/component-versions/{id}}</li>
 *   <li>Review the list of affected assemblies.</li>
 *   <li>Update affected assemblies to use a newer version.</li>
 *   <li>Call {@code PATCH /api/components/{cId}/versions/{id}/deprecate}</li>
 * </ol>
 */
@RestController
@RequestMapping("/api/impact-analysis")
@RequiredArgsConstructor
@Tag(name = "Impact Analysis", description = "Analyze the downstream impact of deprecating a component version")
public class ImpactAnalysisController {

    private final ImpactAnalysisService impactAnalysisService;

    @GetMapping("/component-versions/{versionId}")
    @Operation(
            summary = "Analyze impact of a specific component version",
            description = "Returns all assemblies currently using this version. Run BEFORE deprecating."
    )
    public ResponseEntity<ImpactAnalysisResponse> analyzeVersionImpact(@PathVariable Long versionId) {
        return ResponseEntity.ok(impactAnalysisService.analyzeVersionImpact(versionId));
    }

    @GetMapping("/components/{componentId}")
    @Operation(
            summary = "Analyze impact of all versions of a component",
            description = "Returns all assemblies using ANY version of the given component."
    )
    public ResponseEntity<ImpactAnalysisResponse> analyzeComponentImpact(@PathVariable Long componentId) {
        return ResponseEntity.ok(impactAnalysisService.analyzeComponentImpact(componentId));
    }
}
