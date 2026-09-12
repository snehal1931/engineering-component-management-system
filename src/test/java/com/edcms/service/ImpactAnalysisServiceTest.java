package com.edcms.service;

import com.edcms.dto.response.ImpactAnalysisResponse;
import com.edcms.entity.*;
import com.edcms.enums.ComponentStatus;
import com.edcms.enums.ProjectStatus;
import com.edcms.enums.Role;
import com.edcms.enums.VersionStatus;
import com.edcms.repository.AssemblyComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ImpactAnalysisService}.
 *
 * <p>Tests the core question: "When I deprecate a component version,
 * which assemblies are affected?"
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ImpactAnalysisService Unit Tests")
class ImpactAnalysisServiceTest {

    @Mock
    private AssemblyComponentRepository assemblyComponentRepository;

    @Mock
    private ComponentVersionService versionService;

    @Mock
    private ComponentService componentService;

    @InjectMocks
    private ImpactAnalysisService impactAnalysisService;

    private User testUser;
    private MechanicalComponent motorComp;
    private ComponentVersion batteryV1;
    private Project project1;
    private Project project2;
    private Assembly assembly1;
    private Assembly assembly2;
    private Assembly assembly3;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).username("admin").email("a@a.com").passwordHash("h").role(Role.ADMIN)
                .build();

        motorComp = MechanicalComponent.builder()
                .id(10L).componentCode("ELEC-BATT-001").name("Battery Pack")
                .status(ComponentStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        batteryV1 = ComponentVersion.builder()
                .id(5L).component(motorComp).versionNumber("1.0")
                .status(VersionStatus.APPROVED).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        project1 = Project.builder()
                .id(100L).name("EV Prototype").status(ProjectStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        project2 = Project.builder()
                .id(101L).name("Test Bench").status(ProjectStatus.ACTIVE)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        assembly1 = Assembly.builder()
                .id(200L).name("Powertrain Assembly").project(project1)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        assembly2 = Assembly.builder()
                .id(201L).name("Test Assembly").project(project1)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();

        assembly3 = Assembly.builder()
                .id(202L).name("Prototype Assembly").project(project2)
                .createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now()).build();
    }

    // ─── analyzeVersionImpact ─────────────────────────────────────────────────

    @Test
    @DisplayName("analyzeVersionImpact: battery v1.0 used by one assembly")
    void analyzeVersionImpact_usedByOneAssembly() {
        // Arrange
        AssemblyComponent ac1 = AssemblyComponent.builder()
                .id(1L).assembly(assembly1).componentVersion(batteryV1)
                .quantity(1).addedAt(LocalDateTime.now()).build();

        when(versionService.findVersionEntityById(5L)).thenReturn(batteryV1);
        when(assemblyComponentRepository.findByComponentVersionId(5L)).thenReturn(List.of(ac1));

        // Act
        ImpactAnalysisResponse response = impactAnalysisService.analyzeVersionImpact(5L);

        // Assert
        assertThat(response.getComponentVersionId()).isEqualTo(5L);
        assertThat(response.getVersionNumber()).isEqualTo("1.0");
        assertThat(response.getComponentCode()).isEqualTo("ELEC-BATT-001");
        assertThat(response.getTotalAffectedAssemblies()).isEqualTo(1);
        assertThat(response.getAffectedAssemblies()).hasSize(1);
        assertThat(response.getAffectedAssemblies().get(0).getAssemblyName()).isEqualTo("Powertrain Assembly");
        assertThat(response.getAffectedAssemblies().get(0).getProjectName()).isEqualTo("EV Prototype");
    }

    @Test
    @DisplayName("analyzeVersionImpact: battery v1.0 used by multiple assemblies")
    void analyzeVersionImpact_usedByMultipleAssemblies() {
        // Arrange: battery v1.0 is in 3 different assemblies
        AssemblyComponent ac1 = AssemblyComponent.builder()
                .id(1L).assembly(assembly1).componentVersion(batteryV1)
                .quantity(1).addedAt(LocalDateTime.now()).build();
        AssemblyComponent ac2 = AssemblyComponent.builder()
                .id(2L).assembly(assembly2).componentVersion(batteryV1)
                .quantity(2).addedAt(LocalDateTime.now()).build();
        AssemblyComponent ac3 = AssemblyComponent.builder()
                .id(3L).assembly(assembly3).componentVersion(batteryV1)
                .quantity(1).addedAt(LocalDateTime.now()).build();

        when(versionService.findVersionEntityById(5L)).thenReturn(batteryV1);
        when(assemblyComponentRepository.findByComponentVersionId(5L))
                .thenReturn(List.of(ac1, ac2, ac3));

        // Act
        ImpactAnalysisResponse response = impactAnalysisService.analyzeVersionImpact(5L);

        // Assert
        assertThat(response.getTotalAffectedAssemblies()).isEqualTo(3);
        assertThat(response.getAffectedAssemblies()).hasSize(3);

        // Verify all assemblies are listed
        List<String> assemblyNames = response.getAffectedAssemblies().stream()
                .map(ImpactAnalysisResponse.AffectedAssemblyInfo::getAssemblyName)
                .toList();
        assertThat(assemblyNames).containsExactlyInAnyOrder(
                "Powertrain Assembly", "Test Assembly", "Prototype Assembly");
    }

    @Test
    @DisplayName("analyzeVersionImpact: version not used by any assembly — empty result")
    void analyzeVersionImpact_notUsedAnywhere_emptyResult() {
        // Arrange: battery v1.0 is not in any assembly
        when(versionService.findVersionEntityById(5L)).thenReturn(batteryV1);
        when(assemblyComponentRepository.findByComponentVersionId(5L)).thenReturn(Collections.emptyList());

        // Act
        ImpactAnalysisResponse response = impactAnalysisService.analyzeVersionImpact(5L);

        // Assert
        assertThat(response.getTotalAffectedAssemblies()).isEqualTo(0);
        assertThat(response.getAffectedAssemblies()).isEmpty();
        assertThat(response.getComponentCode()).isEqualTo("ELEC-BATT-001");
    }

    // ─── analyzeComponentImpact ───────────────────────────────────────────────

    @Test
    @DisplayName("analyzeComponentImpact: returns all version usages for component")
    void analyzeComponentImpact_returnsAllVersionUsages() {
        // Arrange: two different battery versions used in different assemblies
        ComponentVersion batteryV2 = ComponentVersion.builder()
                .id(6L).component(motorComp).versionNumber("2.0")
                .status(VersionStatus.APPROVED).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        AssemblyComponent ac1 = AssemblyComponent.builder()
                .id(1L).assembly(assembly1).componentVersion(batteryV1)
                .quantity(1).addedAt(LocalDateTime.now()).build();
        AssemblyComponent ac2 = AssemblyComponent.builder()
                .id(2L).assembly(assembly2).componentVersion(batteryV2)
                .quantity(1).addedAt(LocalDateTime.now()).build();

        when(componentService.findComponentEntityById(10L)).thenReturn(motorComp);
        when(assemblyComponentRepository.findByComponentId(10L)).thenReturn(List.of(ac1, ac2));

        // Act
        ImpactAnalysisResponse response = impactAnalysisService.analyzeComponentImpact(10L);

        // Assert
        assertThat(response.getTotalAffectedAssemblies()).isEqualTo(2);
        assertThat(response.getVersionNumber()).isEqualTo("ALL VERSIONS");
        assertThat(response.getComponentCode()).isEqualTo("ELEC-BATT-001");
    }

    @Test
    @DisplayName("analyzeComponentImpact: component with no assemblies — zero impact")
    void analyzeComponentImpact_noUsages_returnsZeroImpact() {
        // Arrange
        when(componentService.findComponentEntityById(10L)).thenReturn(motorComp);
        when(assemblyComponentRepository.findByComponentId(10L)).thenReturn(Collections.emptyList());

        // Act
        ImpactAnalysisResponse response = impactAnalysisService.analyzeComponentImpact(10L);

        // Assert
        assertThat(response.getTotalAffectedAssemblies()).isEqualTo(0);
        assertThat(response.getAffectedAssemblies()).isEmpty();
        // When no usages, componentCode defaults to "ID:10" per service logic
        assertThat(response.getComponentCode()).isEqualTo("ID:10");
    }
}
