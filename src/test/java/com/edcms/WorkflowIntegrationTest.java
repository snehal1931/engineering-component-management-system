package com.edcms;

import com.edcms.dto.request.*;
import com.edcms.dto.response.*;
import com.edcms.enums.*;
import com.edcms.exception.CircularDependencyException;
import com.edcms.exception.CompatibilityException;
import com.edcms.exception.InvalidVersionStatusException;
import com.edcms.exception.InvalidVersionTransitionException;
import com.edcms.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the 4 most critical workflows in EDCMS.
 *
 * <p><b>Database:</b> H2 in-memory (profile "test"). Schema is auto-created
 * by Hibernate (ddl-auto=create-drop) at the start of each test class.
 *
 * <p><b>Test isolation:</b> Each test uses uniquely-named entities to avoid
 * conflicts. No @Transactional is used at the class level because Spring's
 * transaction boundary would prevent service-level nested @Transactional calls
 * from flushing and seeing each other's changes within the same test.
 *
 * <p>These tests exercise the full Spring context — controllers, services,
 * repositories, and the JPA layer — using real beans, not mocks.
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Integration Tests — 4 Core Workflows")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowIntegrationTest {

    @Autowired private UserService userService;
    @Autowired private ProjectService projectService;
    @Autowired private ComponentService componentService;
    @Autowired private ComponentVersionService componentVersionService;
    @Autowired private AssemblyService assemblyService;
    @Autowired private DependencyService dependencyService;
    @Autowired private CompatibilityService compatibilityService;
    @Autowired private ImpactAnalysisService impactAnalysisService;

    // ─── Shared state created per-test via setUp ──────────────────────────────

    private Long adminUserId;
    private Long engineerUserId;

    @BeforeEach
    void setUp() {
        // Create a shared admin user
        CreateUserRequest adminReq = new CreateUserRequest();
        adminReq.setUsername("admin_" + System.nanoTime()); // unique per test
        adminReq.setEmail("admin_" + System.nanoTime() + "@edcms.com");
        adminReq.setPassword("password123");
        adminReq.setRole(Role.ADMIN);
        adminUserId = userService.createUser(adminReq).getId();

        // Create a shared engineer user
        CreateUserRequest engReq = new CreateUserRequest();
        engReq.setUsername("engineer_" + System.nanoTime());
        engReq.setEmail("engineer_" + System.nanoTime() + "@edcms.com");
        engReq.setPassword("password456");
        engReq.setRole(Role.ENGINEER);
        engineerUserId = userService.createUser(engReq).getId();
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WORKFLOW 1: Create project → Component → Version → Assembly → Add version
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(1)
    @DisplayName("Workflow 1: Full BOM lifecycle — project → component → version → assembly → add")
    void workflow1_fullBomLifecycle() {
        // Step 1: Create project
        CreateProjectRequest projReq = new CreateProjectRequest();
        projReq.setName("EV Prototype Project");
        projReq.setDescription("Phase 1 electric vehicle prototype");
        projReq.setCreatedById(adminUserId);
        ProjectResponse project = projectService.createProject(projReq);
        assertThat(project.getId()).isNotNull();
        assertThat(project.getStatus()).isEqualTo(ProjectStatus.PLANNING);

        // Step 2: Create mechanical component (Motor)
        CreateComponentRequest compReq = new CreateComponentRequest();
        compReq.setComponentCode("MECH-MOTOR-W1A");
        compReq.setName("Workflow1 Brushless Motor");
        compReq.setComponentType(ComponentType.MECHANICAL);
        compReq.setCreatedById(engineerUserId);
        compReq.setMaterial("Aluminium Alloy");
        ComponentResponse component = componentService.createComponent(compReq);
        assertThat(component.getId()).isNotNull();
        assertThat(component.getComponentType()).isEqualTo(ComponentType.MECHANICAL);
        assertThat(component.getStatus()).isEqualTo(ComponentStatus.ACTIVE);

        // Step 3: Create component version (starts as DRAFT)
        CreateComponentVersionRequest verReq = new CreateComponentVersionRequest();
        verReq.setVersionNumber("2.0");
        verReq.setReleaseNotes("Initial production release");
        verReq.setCreatedById(engineerUserId);
        ComponentVersionResponse version = componentVersionService.createVersion(component.getId(), verReq);
        assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT);

        // Step 4: Approve the version (DRAFT → APPROVED)
        ComponentVersionResponse approved = componentVersionService.approveVersion(version.getId());
        assertThat(approved.getStatus()).isEqualTo(VersionStatus.APPROVED);

        // Step 5: Create assembly under the project
        CreateAssemblyRequest assemblyReq = new CreateAssemblyRequest();
        assemblyReq.setName("Powertrain Assembly W1");
        assemblyReq.setDescription("Main powertrain subassembly");
        assemblyReq.setCreatedById(adminUserId);
        AssemblyResponse assembly = assemblyService.createAssembly(project.getId(), assemblyReq);
        assertThat(assembly.getId()).isNotNull();
        assertThat(assembly.getProjectId()).isEqualTo(project.getId());
        assertThat(assembly.getComponents()).isEmpty();

        // Step 6: Add approved version to the assembly
        AddComponentToAssemblyRequest addReq = new AddComponentToAssemblyRequest();
        addReq.setComponentVersionId(version.getId());
        addReq.setQuantity(1);
        addReq.setNotes("Primary drive motor");
        AssemblyResponse updatedAssembly = assemblyService.addComponentVersion(assembly.getId(), addReq);
        assertThat(updatedAssembly.getComponents()).hasSize(1);
        assertThat(updatedAssembly.getComponents().get(0).getVersionNumber()).isEqualTo("2.0");
        assertThat(updatedAssembly.getComponents().get(0).getComponentCode()).isEqualTo("MECH-MOTOR-W1A");
    }

    @Test
    @Order(2)
    @DisplayName("Workflow 1 (edge): cannot add DRAFT version to assembly")
    void workflow1_cannotAddDraftVersionToAssembly() {
        // Create component and project
        CreateComponentRequest compReq = new CreateComponentRequest();
        compReq.setComponentCode("MECH-MOTOR-W1B");
        compReq.setName("Draft Motor");
        compReq.setComponentType(ComponentType.MECHANICAL);
        compReq.setCreatedById(engineerUserId);
        ComponentResponse comp = componentService.createComponent(compReq);

        CreateComponentVersionRequest verReq = new CreateComponentVersionRequest();
        verReq.setVersionNumber("1.0");
        verReq.setCreatedById(engineerUserId);
        ComponentVersionResponse draftVersion = componentVersionService.createVersion(comp.getId(), verReq);
        // Note: NOT approved — stays DRAFT

        CreateProjectRequest projReq = new CreateProjectRequest();
        projReq.setName("Test Project W1B");
        projReq.setCreatedById(adminUserId);
        ProjectResponse project = projectService.createProject(projReq);

        CreateAssemblyRequest assemblyReq = new CreateAssemblyRequest();
        assemblyReq.setName("Test Assembly W1B");
        assemblyReq.setCreatedById(adminUserId);
        AssemblyResponse assembly = assemblyService.createAssembly(project.getId(), assemblyReq);

        AddComponentToAssemblyRequest addReq = new AddComponentToAssemblyRequest();
        addReq.setComponentVersionId(draftVersion.getId());
        addReq.setQuantity(1);

        // Must be rejected: version is DRAFT
        assertThatThrownBy(() -> assemblyService.addComponentVersion(assembly.getId(), addReq))
                .isInstanceOf(InvalidVersionStatusException.class)
                .hasMessageContaining("DRAFT");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WORKFLOW 2: Compatibility check — Motor v2.0 + Controller v2.0 → fails
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(3)
    @DisplayName("Workflow 2: Motor v2.0 incompatible with Controller v2.0")
    void workflow2_incompatibilityCheck_motorV2WithControllerV2_fails() {
        // Create Motor v2.0
        CreateComponentRequest motorReq = new CreateComponentRequest();
        motorReq.setComponentCode("MECH-MOTOR-W2A");
        motorReq.setName("Motor W2");
        motorReq.setComponentType(ComponentType.MECHANICAL);
        motorReq.setCreatedById(engineerUserId);
        ComponentResponse motor = componentService.createComponent(motorReq);

        CreateComponentVersionRequest motorVerReq = new CreateComponentVersionRequest();
        motorVerReq.setVersionNumber("2.0");
        motorVerReq.setCreatedById(engineerUserId);
        ComponentVersionResponse motorV2 = componentVersionService.createVersion(motor.getId(), motorVerReq);
        componentVersionService.approveVersion(motorV2.getId());

        // Create Controller v2.0
        CreateComponentRequest ctrlReq = new CreateComponentRequest();
        ctrlReq.setComponentCode("SW-CTRL-W2A");
        ctrlReq.setName("Controller W2");
        ctrlReq.setComponentType(ComponentType.SOFTWARE);
        ctrlReq.setCreatedById(engineerUserId);
        ComponentResponse controller = componentService.createComponent(ctrlReq);

        CreateComponentVersionRequest ctrlVerReq = new CreateComponentVersionRequest();
        ctrlVerReq.setVersionNumber("2.0");
        ctrlVerReq.setCreatedById(engineerUserId);
        ComponentVersionResponse ctrlV2 = componentVersionService.createVersion(controller.getId(), ctrlVerReq);
        componentVersionService.approveVersion(ctrlV2.getId());

        // Create compatibility rule: Motor v2.0 ✗ Controller v2.0 (incompatible)
        CreateCompatibilityRuleRequest ruleReq = new CreateCompatibilityRuleRequest();
        ruleReq.setVersionAId(motorV2.getId());
        ruleReq.setVersionBId(ctrlV2.getId());
        ruleReq.setIsCompatible(false);
        ruleReq.setRuleDescription("Motor v2.0 requires CAN 2.0 protocol. Controller v2.0 only supports CAN 1.0. Communication failure occurs in production.");
        ruleReq.setCreatedById(adminUserId);
        compatibilityService.createRule(ruleReq);

        // Create project and assembly with Motor v2.0
        CreateProjectRequest projReq = new CreateProjectRequest();
        projReq.setName("Test Project W2");
        projReq.setCreatedById(adminUserId);
        ProjectResponse project = projectService.createProject(projReq);

        CreateAssemblyRequest assemblyReq = new CreateAssemblyRequest();
        assemblyReq.setName("Powertrain W2");
        assemblyReq.setCreatedById(adminUserId);
        AssemblyResponse assembly = assemblyService.createAssembly(project.getId(), assemblyReq);

        // Add Motor v2.0 first (succeeds — empty assembly, no rules to violate)
        AddComponentToAssemblyRequest addMotor = new AddComponentToAssemblyRequest();
        addMotor.setComponentVersionId(motorV2.getId());
        addMotor.setQuantity(1);
        assemblyService.addComponentVersion(assembly.getId(), addMotor);

        // Now try to add Controller v2.0 — must FAIL due to compatibility rule
        AddComponentToAssemblyRequest addCtrl = new AddComponentToAssemblyRequest();
        addCtrl.setComponentVersionId(ctrlV2.getId());
        addCtrl.setQuantity(1);

        assertThatThrownBy(() -> assemblyService.addComponentVersion(assembly.getId(), addCtrl))
                .isInstanceOf(CompatibilityException.class)
                .hasMessageContaining("compatibility violation");
    }

    @Test
    @Order(4)
    @DisplayName("Workflow 2: Motor v2.0 compatible with Controller v3.0 — succeeds")
    void workflow2_compatibilityCheck_motorV2WithControllerV3_succeeds() {
        // Create Motor v2.0
        CreateComponentRequest motorReq = new CreateComponentRequest();
        motorReq.setComponentCode("MECH-MOTOR-W2B");
        motorReq.setName("Motor W2B");
        motorReq.setComponentType(ComponentType.MECHANICAL);
        motorReq.setCreatedById(engineerUserId);
        ComponentResponse motor = componentService.createComponent(motorReq);

        CreateComponentVersionRequest motorVerReq = new CreateComponentVersionRequest();
        motorVerReq.setVersionNumber("2.0");
        motorVerReq.setCreatedById(engineerUserId);
        ComponentVersionResponse motorV2 = componentVersionService.createVersion(motor.getId(), motorVerReq);
        componentVersionService.approveVersion(motorV2.getId());

        // Create Controller v3.0
        CreateComponentRequest ctrlReq = new CreateComponentRequest();
        ctrlReq.setComponentCode("SW-CTRL-W2B");
        ctrlReq.setName("Controller W2B");
        ctrlReq.setComponentType(ComponentType.SOFTWARE);
        ctrlReq.setCreatedById(engineerUserId);
        ComponentResponse controller = componentService.createComponent(ctrlReq);

        CreateComponentVersionRequest ctrlVerReq = new CreateComponentVersionRequest();
        ctrlVerReq.setVersionNumber("3.0");
        ctrlVerReq.setCreatedById(engineerUserId);
        ComponentVersionResponse ctrlV3 = componentVersionService.createVersion(controller.getId(), ctrlVerReq);
        componentVersionService.approveVersion(ctrlV3.getId());

        // Create compatibility rule: Motor v2.0 ✓ Controller v3.0 (compatible)
        CreateCompatibilityRuleRequest ruleReq = new CreateCompatibilityRuleRequest();
        ruleReq.setVersionAId(motorV2.getId());
        ruleReq.setVersionBId(ctrlV3.getId());
        ruleReq.setIsCompatible(true);
        ruleReq.setRuleDescription("Motor v2.0 is fully compatible with Controller v3.0 CAN 2.0 interface.");
        ruleReq.setCreatedById(adminUserId);
        compatibilityService.createRule(ruleReq);

        // Set up project + assembly + add Motor v2.0
        CreateProjectRequest projReq = new CreateProjectRequest();
        projReq.setName("Test Project W2B");
        projReq.setCreatedById(adminUserId);
        ProjectResponse project = projectService.createProject(projReq);

        CreateAssemblyRequest assemblyReq = new CreateAssemblyRequest();
        assemblyReq.setName("Powertrain W2B");
        assemblyReq.setCreatedById(adminUserId);
        AssemblyResponse assembly = assemblyService.createAssembly(project.getId(), assemblyReq);

        AddComponentToAssemblyRequest addMotor = new AddComponentToAssemblyRequest();
        addMotor.setComponentVersionId(motorV2.getId());
        addMotor.setQuantity(1);
        assemblyService.addComponentVersion(assembly.getId(), addMotor);

        // Add Controller v3.0 — must SUCCEED
        AddComponentToAssemblyRequest addCtrl = new AddComponentToAssemblyRequest();
        addCtrl.setComponentVersionId(ctrlV3.getId());
        addCtrl.setQuantity(1);

        AssemblyResponse result = assemblyService.addComponentVersion(assembly.getId(), addCtrl);
        assertThat(result.getComponents()).hasSize(2);
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WORKFLOW 3: Circular dependency — A→B→C→A must be rejected
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(5)
    @DisplayName("Workflow 3: Circular dependency A→B→C→A — must be rejected")
    void workflow3_circularDependency_A_B_C_A_rejected() {
        // Create 3 components: Motor, Controller, Firmware
        ComponentResponse motor = createComponent("MECH-MOTOR-W3", "Motor W3", ComponentType.MECHANICAL);
        ComponentResponse controller = createComponent("SW-CTRL-W3", "Controller W3", ComponentType.SOFTWARE);
        ComponentResponse firmware = createComponent("SW-FW-W3", "Firmware W3", ComponentType.SOFTWARE);

        // Create versions for each
        ComponentVersionResponse motorV2 = createAndApproveVersion(motor.getId(), "2.0");
        ComponentVersionResponse ctrlV3 = createAndApproveVersion(controller.getId(), "3.0");
        ComponentVersionResponse fwV1 = createAndApproveVersion(firmware.getId(), "1.0");

        // Establish: Motor → Controller
        CreateDependencyRequest depAtoB = new CreateDependencyRequest();
        depAtoB.setSourceVersionId(motorV2.getId());
        depAtoB.setTargetVersionId(ctrlV3.getId());
        depAtoB.setDependencyType(DependencyType.REQUIRES);
        depAtoB.setCreatedById(adminUserId);
        DependencyResponse dep1 = dependencyService.createDependency(depAtoB);
        assertThat(dep1.getId()).isNotNull();

        // Establish: Controller → Firmware
        CreateDependencyRequest depBtoC = new CreateDependencyRequest();
        depBtoC.setSourceVersionId(ctrlV3.getId());
        depBtoC.setTargetVersionId(fwV1.getId());
        depBtoC.setDependencyType(DependencyType.REQUIRES);
        depBtoC.setCreatedById(adminUserId);
        DependencyResponse dep2 = dependencyService.createDependency(depBtoC);
        assertThat(dep2.getId()).isNotNull();

        // Attempt: Firmware → Motor (would close the cycle A→B→C→A)
        CreateDependencyRequest depCtoA = new CreateDependencyRequest();
        depCtoA.setSourceVersionId(fwV1.getId());
        depCtoA.setTargetVersionId(motorV2.getId());
        depCtoA.setDependencyType(DependencyType.REQUIRES);
        depCtoA.setCreatedById(adminUserId);

        assertThatThrownBy(() -> dependencyService.createDependency(depCtoA))
                .isInstanceOf(CircularDependencyException.class)
                .hasMessageContaining("circular dependency");
    }

    // ═══════════════════════════════════════════════════════════════════════════
    // WORKFLOW 4: Version state machine — transitions enforced
    // ═══════════════════════════════════════════════════════════════════════════

    @Test
    @Order(6)
    @DisplayName("Workflow 4: DRAFT→APPROVED→DEPRECATED — valid lifecycle")
    void workflow4_versionLifecycle_validTransitions() {
        ComponentResponse comp = createComponent("ELEC-BATT-W4A", "Battery W4", ComponentType.ELECTRICAL);

        CreateComponentVersionRequest verReq = new CreateComponentVersionRequest();
        verReq.setVersionNumber("1.0");
        verReq.setReleaseNotes("Initial release");
        verReq.setCreatedById(engineerUserId);
        ComponentVersionResponse version = componentVersionService.createVersion(comp.getId(), verReq);
        assertThat(version.getStatus()).isEqualTo(VersionStatus.DRAFT);

        // DRAFT → APPROVED
        ComponentVersionResponse approved = componentVersionService.approveVersion(version.getId());
        assertThat(approved.getStatus()).isEqualTo(VersionStatus.APPROVED);

        // APPROVED → DEPRECATED
        ComponentVersionResponse deprecated = componentVersionService.deprecateVersion(version.getId());
        assertThat(deprecated.getStatus()).isEqualTo(VersionStatus.DEPRECATED);
    }

    @Test
    @Order(7)
    @DisplayName("Workflow 4: DEPRECATED → APPROVED — invalid, must be rejected")
    void workflow4_deprecatedToApproved_rejected() {
        ComponentResponse comp = createComponent("ELEC-BATT-W4B", "Battery W4B", ComponentType.ELECTRICAL);

        CreateComponentVersionRequest verReq = new CreateComponentVersionRequest();
        verReq.setVersionNumber("1.0");
        verReq.setCreatedById(engineerUserId);
        ComponentVersionResponse version = componentVersionService.createVersion(comp.getId(), verReq);

        // DRAFT → APPROVED → DEPRECATED
        componentVersionService.approveVersion(version.getId());
        componentVersionService.deprecateVersion(version.getId());

        // Attempt DEPRECATED → APPROVED — must fail
        assertThatThrownBy(() -> componentVersionService.approveVersion(version.getId()))
                .isInstanceOf(InvalidVersionTransitionException.class)
                .hasMessageContaining("DEPRECATED");
    }

    @Test
    @Order(8)
    @DisplayName("Workflow 4: APPROVED → DRAFT — invalid (rollback not allowed)")
    void workflow4_approvedToDraft_rejected() {
        ComponentResponse comp = createComponent("ELEC-BATT-W4C", "Battery W4C", ComponentType.ELECTRICAL);

        CreateComponentVersionRequest verReq = new CreateComponentVersionRequest();
        verReq.setVersionNumber("1.0");
        verReq.setCreatedById(engineerUserId);
        ComponentVersionResponse version = componentVersionService.createVersion(comp.getId(), verReq);
        componentVersionService.approveVersion(version.getId());

        // Direct test of the state machine — APPROVED can only go to DEPRECATED
        assertThatThrownBy(
                () -> componentVersionService.validateTransition(VersionStatus.APPROVED, VersionStatus.DRAFT))
                .isInstanceOf(InvalidVersionTransitionException.class);
    }

    @Test
    @Order(9)
    @DisplayName("Workflow 4: Impact analysis — deprecated version used by 2 assemblies")
    void workflow4_impactAnalysis_deprecatedVersionUsedBy2Assemblies() {
        ComponentResponse comp = createComponent("ELEC-BATT-W4D", "Battery W4D", ComponentType.ELECTRICAL);

        CreateComponentVersionRequest verReq = new CreateComponentVersionRequest();
        verReq.setVersionNumber("1.0");
        verReq.setCreatedById(engineerUserId);
        ComponentVersionResponse version = componentVersionService.createVersion(comp.getId(), verReq);
        componentVersionService.approveVersion(version.getId());

        // Create 2 projects + 2 assemblies, each using this version
        CreateProjectRequest projReq1 = new CreateProjectRequest();
        projReq1.setName("Project Alpha W4D");
        projReq1.setCreatedById(adminUserId);
        ProjectResponse project1 = projectService.createProject(projReq1);

        CreateProjectRequest projReq2 = new CreateProjectRequest();
        projReq2.setName("Project Beta W4D");
        projReq2.setCreatedById(adminUserId);
        ProjectResponse project2 = projectService.createProject(projReq2);

        CreateAssemblyRequest asmReq1 = new CreateAssemblyRequest();
        asmReq1.setName("Assembly Alpha W4D");
        asmReq1.setCreatedById(adminUserId);
        AssemblyResponse assembly1 = assemblyService.createAssembly(project1.getId(), asmReq1);

        CreateAssemblyRequest asmReq2 = new CreateAssemblyRequest();
        asmReq2.setName("Assembly Beta W4D");
        asmReq2.setCreatedById(adminUserId);
        AssemblyResponse assembly2 = assemblyService.createAssembly(project2.getId(), asmReq2);

        // Add the version to both assemblies
        AddComponentToAssemblyRequest addReq = new AddComponentToAssemblyRequest();
        addReq.setComponentVersionId(version.getId());
        addReq.setQuantity(1);
        assemblyService.addComponentVersion(assembly1.getId(), addReq);
        assemblyService.addComponentVersion(assembly2.getId(), addReq);

        // Run impact analysis — before deprecating, check what's affected
        ImpactAnalysisResponse impact = impactAnalysisService.analyzeVersionImpact(version.getId());
        assertThat(impact.getTotalAffectedAssemblies()).isEqualTo(2);
        assertThat(impact.getVersionNumber()).isEqualTo("1.0");
        assertThat(impact.getComponentCode()).isEqualTo("ELEC-BATT-W4D");

        // Now deprecate
        componentVersionService.deprecateVersion(version.getId());

        // The assemblies still reference the deprecated version — impact analysis still shows 2
        ImpactAnalysisResponse impactAfter = impactAnalysisService.analyzeVersionImpact(version.getId());
        assertThat(impactAfter.getTotalAffectedAssemblies()).isEqualTo(2);
    }

    // ─── Helper methods ──────────────────────────────────────────────────────

    private ComponentResponse createComponent(String code, String name, ComponentType type) {
        CreateComponentRequest req = new CreateComponentRequest();
        req.setComponentCode(code);
        req.setName(name);
        req.setComponentType(type);
        req.setCreatedById(engineerUserId);
        return componentService.createComponent(req);
    }

    private ComponentVersionResponse createAndApproveVersion(Long componentId, String versionNumber) {
        CreateComponentVersionRequest req = new CreateComponentVersionRequest();
        req.setVersionNumber(versionNumber);
        req.setCreatedById(engineerUserId);
        ComponentVersionResponse created = componentVersionService.createVersion(componentId, req);
        return componentVersionService.approveVersion(created.getId());
    }
}
