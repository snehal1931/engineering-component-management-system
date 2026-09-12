package com.edcms.controller;

import com.edcms.dto.request.CompatibilityCheckRequest;
import com.edcms.dto.request.CreateCompatibilityRuleRequest;
import com.edcms.dto.response.CompatibilityCheckResponse;
import com.edcms.dto.response.CompatibilityRuleResponse;
import com.edcms.service.CompatibilityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for compatibility rules and compatibility checking.
 *
 * <p><b>Endpoints:</b>
 * <pre>
 *   POST   /api/compatibility-rules         → Declare a compatibility rule between two versions
 *   GET    /api/compatibility-rules         → List all declared compatibility rules
 *   POST   /api/compatibility/check         → Check if a set of versions are mutually compatible
 * </pre>
 *
 * <p>The {@code /check} endpoint is the primary consumer-facing API for compatibility verification.
 * Engineers provide a list of version IDs they plan to use together and get back
 * a compatible/incompatible decision with violation details.
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Compatibility", description = "Component version compatibility rules and checks")
public class CompatibilityController {

    private final CompatibilityService compatibilityService;

    @PostMapping("/api/compatibility-rules")
    @Operation(
            summary = "Declare a compatibility rule",
            description = "Rules are symmetric — declaring (A,B) covers both (A,B) and (B,A) directions."
    )
    public ResponseEntity<CompatibilityRuleResponse> createRule(
            @Valid @RequestBody CreateCompatibilityRuleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(compatibilityService.createRule(request));
    }

    @GetMapping("/api/compatibility-rules")
    @Operation(summary = "List all declared compatibility rules")
    public ResponseEntity<List<CompatibilityRuleResponse>> getAllRules() {
        return ResponseEntity.ok(compatibilityService.getAllRules());
    }

    /**
     * Check if a set of component versions are mutually compatible.
     *
     * <p>Provide a list of component version IDs in the request body.
     * The system checks every unique pair against declared compatibility rules.
     *
     * <p>Response includes:
     * <ul>
     *   <li>{@code allCompatible}: true/false summary</li>
     *   <li>{@code violations}: detailed list of incompatible pairs (if any)</li>
     * </ul>
     *
     * <p><b>Note:</b> A 200 OK response does NOT mean everything is compatible.
     * Always check the {@code allCompatible} flag in the response body.
     */
    @PostMapping("/api/compatibility/check")
    @Operation(
            summary = "Check mutual compatibility of a set of component versions",
            description = "Returns 200 OK with allCompatible=true/false. Check the response body for violations."
    )
    public ResponseEntity<CompatibilityCheckResponse> checkCompatibility(
            @Valid @RequestBody CompatibilityCheckRequest request) {
        return ResponseEntity.ok(compatibilityService.checkVersions(request));
    }
}
