# TESTING.md — EDCMS Test Report

## Overview

This document describes the testing strategy, test cases, bugs found and fixed, and known limitations for the Engineering Design & Component Management System (EDCMS).

---

## Test Statistics

| Category | Count |
|---|---|
| Total tests | 50 |
| Unit tests | 41 |
| Integration tests | 9 |
| Test classes | 6 |
| Bugs found and fixed | 5 |

---

## Test Files

| File | Type | Service Under Test |
|---|---|---|
| `ComponentServiceTest.java` | Unit | `ComponentService` |
| `ComponentVersionServiceTest.java` | Unit | `ComponentVersionService` |
| `DependencyServiceTest.java` | Unit | `DependencyService` |
| `CompatibilityServiceTest.java` | Unit | `CompatibilityService` |
| `ImpactAnalysisServiceTest.java` | Unit | `ImpactAnalysisService` |
| `WorkflowIntegrationTest.java` | Integration | All services + JPA layer |

---

## Unit Test Cases

### ComponentServiceTest (8 tests)

| Test Method | What It Tests |
|---|---|
| `createComponent_mechanical_success` | Creates MECHANICAL component, verifies type + code |
| `createComponent_software_success` | Creates SOFTWARE component, verifies correct entity subclass |
| `createComponent_duplicateCode_throwsDuplicateComponentException` | Duplicate code rejected before DB INSERT |
| `getComponentById_found_returnsResponse` | Happy path for lookup by ID |
| `getComponentById_notFound_throwsComponentNotFoundException` | 404-style exception for missing ID |
| `updateComponent_success_updatesNameDescriptionManufacturer` | Mutable fields updated, immutable unchanged |
| `updateComponent_notFound_throwsComponentNotFoundException` | Update on missing ID |
| `deprecateComponent_success_setsStatusDeprecated` | Entity status field set to DEPRECATED |

### ComponentVersionServiceTest (13 tests)

| Test Method | What It Tests |
|---|---|
| `createVersion_success_createsDraftVersion` | New version always starts as DRAFT |
| `createVersion_duplicateVersionNumber_throwsIllegalArgumentException` | Same version number per component rejected |
| `approveVersion_draftToApproved_validTransition` | DRAFT → APPROVED: legal |
| `approveVersion_approvedToApproved_throwsInvalidVersionTransitionException` | APPROVED → APPROVED: illegal (no-op forbidden) |
| `approveVersion_deprecatedToApproved_throwsInvalidVersionTransitionException` | DEPRECATED → APPROVED: illegal (terminal state) |
| `deprecateVersion_approvedToDeprecated_validTransition` | APPROVED → DEPRECATED: legal |
| `deprecateVersion_draftToDeprecated_validTransition` | DRAFT → DEPRECATED: legal (skip APPROVED) |
| `deprecateVersion_deprecatedToDeprecated_throwsInvalidVersionTransitionException` | DEPRECATED → DEPRECATED: illegal |
| `validateTransition_allValidTransitions_noException` | All 3 valid transitions pass without exception |
| `validateTransition_approvedToDraft_throwsInvalidVersionTransitionException` | APPROVED → DRAFT: illegal rollback |
| `validateTransition_deprecatedToDraft_throwsInvalidVersionTransitionException` | DEPRECATED → DRAFT: illegal |
| `validateTransition_deprecatedToApproved_throwsInvalidVersionTransitionException` | DEPRECATED → APPROVED: illegal |
| `getVersionById_notFound_throwsComponentVersionNotFoundException` | 404 for missing version |

### DependencyServiceTest (6 tests)

| Test Method | What It Tests |
|---|---|
| `createDependency_validEdge_success` | A → B dependency created, no cycle |
| `createDependency_selfDependency_throwsIllegalArgumentException` | A → A rejected |
| `createDependency_duplicateDependency_throwsIllegalArgumentException` | Duplicate A→B rejected |
| `wouldCreateCycle_linearChain_returnsFalse` | A → B chain is safe |
| `wouldCreateCycle_simpleCycle_A_B_C_A_returnsTrue` | Motor→Controller→Firmware + Firmware→Motor = CYCLE |
| `createDependency_circularDependency_throwsCircularDependencyException` | Full end-to-end cycle detection |

### CompatibilityServiceTest (8 tests)

| Test Method | What It Tests |
|---|---|
| `createRule_motorV2IncompatibleWithCtrlV2_success` | Incompatibility rule persisted |
| `createRule_sameVersionForBothSides_throwsIllegalArgumentException` | Self-rule rejected |
| `createRule_duplicateRuleForSamePair_throwsCompatibilityException` | Duplicate rule rejected |
| `checkVersions_motorV2WithCtrlV3_compatible` | No violations found |
| `checkVersions_motorV2WithCtrlV2_incompatible` | 1 violation returned |
| `validateBeforeAdding_noExistingVersions_noExceptionThrown` | Empty assembly always passes |
| `validateBeforeAdding_compatibleNewVersion_noException` | Compatible version accepted |
| `validateBeforeAdding_incompatibleNewVersion_throwsCompatibilityException` | Incompatible version rejected |

### ImpactAnalysisServiceTest (5 tests)

| Test Method | What It Tests |
|---|---|
| `analyzeVersionImpact_usedByOneAssembly` | Single assembly impact |
| `analyzeVersionImpact_usedByMultipleAssemblies` | 3 assemblies across 2 projects |
| `analyzeVersionImpact_notUsedAnywhere_emptyResult` | Zero impact for unused version |
| `analyzeComponentImpact_returnsAllVersionUsages` | Component-level: both versions listed |
| `analyzeComponentImpact_noUsages_returnsZeroImpact` | Zero impact for unused component |

---

## Integration Test Workflows

### WorkflowIntegrationTest (9 tests)

**Workflow 1 — Full BOM Lifecycle (2 tests)**

| Test | Scenario |
|---|---|
| `workflow1_fullBomLifecycle` | Create project → component → version → approve → assembly → add. Verifies chain end-to-end. |
| `workflow1_cannotAddDraftVersionToAssembly` | DRAFT version → assembly rejected with `InvalidVersionStatusException` |

**Workflow 2 — Compatibility Check (2 tests)**

| Test | Scenario |
|---|---|
| `workflow2_incompatibilityCheck_motorV2WithControllerV2_fails` | Motor v2 + Controller v2 flagged as incompatible (explicit rule), second add throws `CompatibilityException` |
| `workflow2_compatibilityCheck_motorV2WithControllerV3_succeeds` | Motor v2 + Controller v3 passes, assembly has 2 components |

**Workflow 3 — Circular Dependency (1 test)**

| Test | Scenario |
|---|---|
| `workflow3_circularDependency_A_B_C_A_rejected` | Motor→Controller + Controller→Firmware established; Firmware→Motor attempt throws `CircularDependencyException` |

**Workflow 4 — Version State Machine (4 tests)**

| Test | Scenario |
|---|---|
| `workflow4_versionLifecycle_validTransitions` | DRAFT→APPROVED→DEPRECATED complete lifecycle |
| `workflow4_deprecatedToApproved_rejected` | DEPRECATED→APPROVED throws `InvalidVersionTransitionException` |
| `workflow4_approvedToDraft_rejected` | APPROVED→DRAFT is illegal rollback |
| `workflow4_impactAnalysis_deprecatedVersionUsedBy2Assemblies` | Battery v1.0 in 2 assemblies, impact analysis shows count=2 |

---

## Bugs Found and Fixed During Testing

### Bug 1 — LazyInitializationException in AssemblyController (CRITICAL)
- **Symptom:** `AssemblyController.addComponentVersion()` accessed `assembly.getAssemblyComponents()` (a LAZY collection) outside a `@Transactional` boundary.
- **Root Cause:** Compatibility check was performed in the controller after the service transaction closed.
- **Fix:** Moved all business rule checks (version status, duplicate, compatibility) into `AssemblyService.addComponentVersion()` under a single `@Transactional`.

### Bug 2 — IllegalArgumentException returning HTTP 500 (HIGH)
- **Symptom:** Services throwing `IllegalArgumentException` for business rule violations (self-dependency, duplicate dependency, duplicate version number) returned HTTP 500.
- **Root Cause:** `GlobalExceptionHandler` had no `@ExceptionHandler(IllegalArgumentException.class)`.
- **Fix:** Added handler returning HTTP 400 Bad Request.

### Bug 3 — AssemblyComponentRepository N+1 Risk (MEDIUM)
- **Symptom:** `ImpactAnalysisService` calls `ac.getAssembly().getProject()` for each result — each access would trigger a separate SQL SELECT (N+1).
- **Root Cause:** Repository queries returned entities without eagerly fetching related entities.
- **Fix:** Added `JOIN FETCH` to both impact analysis queries (`findByComponentVersionId`, `findByComponentId`).

### Bug 4 — componentType returns null after entity creation (MEDIUM)
- **Symptom:** `componentType` field was `null` immediately after `componentRepository.save()`.
- **Root Cause:** The `componentType` field maps to the JPA discriminator column (`insertable=false, updatable=false`). Hibernate does not back-populate this field from the INSERT — it reads it only when loading from DB.
- **Fix:** Added a `findById()` reload after `save()` in `ComponentService.createComponent()`.

### Bug 5 — Stale Hibernate First-Level Cache in addComponentVersion (MEDIUM)
- **Symptom:** `AssemblyResponse` returned from `addComponentVersion` showed 0 components even after successful save.
- **Root Cause:** `findAssemblyEntityById()` within the same transaction returned the same cached `Assembly` entity from Hibernate's first-level cache, with the old (unmodified) `assemblyComponents` collection.
- **Fix:** Instead of reloading the entity (which returns the cache), the new `AssemblyComponent` is added directly to the in-memory collection before building the response.

---

## Code Quality Notes

### N+1 Query Analysis

| Service Method | Status | Notes |
|---|---|---|
| `ImpactAnalysisService.analyzeVersionImpact()` | ✅ Fixed | JOIN FETCH added to repository query |
| `ImpactAnalysisService.analyzeComponentImpact()` | ✅ Fixed | JOIN FETCH added to repository query |
| `AssemblyMapper.toResponse()` | ⚠️ Potential | Accesses `assembly.project.name` — safe if called inside @Transactional |
| `ComponentMapper.toResponse()` | ✅ Safe | No lazy access — all fields accessed are already loaded |

### Transaction Strategy

All write operations are `@Transactional`. All reads are `@Transactional(readOnly = true)`. The `enable_lazy_load_no_trans = false` setting in `application.yml` ensures lazy loading never silently succeeds outside a transaction.

---

## Known Limitations

1. **No authentication or authorization enforcement at HTTP level.** The `createdById` field is passed in the request body. In production, this would come from the JWT token.

2. **Passwords stored as plain text.** `UserService.createUser()` stores the raw password in `passwordHash`. This is intentional for the project scope (Spring Security BCrypt hashing would be the production fix).

3. **No pagination.** `getAllComponents()` and similar list endpoints return all records. For large catalogs, Spring Data's `Page<T>` should be used.

4. **Compatibility check is O(n²).** `DefaultCompatibilityChecker.checkAll()` checks every unique pair. For assemblies with 100+ components, a cached approach would be more efficient.

5. **Integration tests write to the H2 database and don't clean up.** Since `application-test.yml` uses `create-drop`, the schema is fresh per test run, but test methods within a run may see data from prior tests (because we removed `@Transactional` at the class level to fix transaction boundary issues).
