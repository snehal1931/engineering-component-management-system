package com.edcms.service;

import com.edcms.dto.request.CreateDependencyRequest;
import com.edcms.dto.response.DependencyResponse;
import com.edcms.entity.Component;
import com.edcms.entity.ComponentVersion;
import com.edcms.entity.Dependency;
import com.edcms.entity.MechanicalComponent;
import com.edcms.entity.SoftwareComponent;
import com.edcms.entity.User;
import com.edcms.enums.ComponentStatus;
import com.edcms.enums.DependencyType;
import com.edcms.enums.Role;
import com.edcms.enums.VersionStatus;
import com.edcms.exception.CircularDependencyException;
import com.edcms.repository.DependencyRepository;
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
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DependencyService}.
 *
 * <p>The most important tests here are the circular dependency detection tests.
 * The DFS algorithm is tested with:
 * <ul>
 *   <li>No cycle: A → B — safe</li>
 *   <li>Simple cycle: A → B → C → A — must be rejected</li>
 *   <li>The exact scenario: Motor→Controller→Firmware→Motor</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DependencyService Unit Tests")
class DependencyServiceTest {

    @Mock
    private DependencyRepository dependencyRepository;

    @Mock
    private ComponentVersionService versionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private DependencyService dependencyService;

    private User testUser;
    private Component motorComp;
    private Component controllerComp;
    private Component firmwareComp;

    private ComponentVersion motorV2;       // id=1
    private ComponentVersion controllerV3;  // id=2
    private ComponentVersion firmwareV1;    // id=3

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).username("admin").email("a@a.com").passwordHash("h").role(Role.ADMIN)
                .build();

        motorComp = MechanicalComponent.builder()
                .id(10L).componentCode("MECH-MOTOR-001").name("Motor")
                .status(ComponentStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        controllerComp = SoftwareComponent.builder()
                .id(11L).componentCode("SW-CTRL-001").name("Controller")
                .status(ComponentStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        firmwareComp = SoftwareComponent.builder()
                .id(12L).componentCode("SW-FW-001").name("Firmware")
                .status(ComponentStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        motorV2 = ComponentVersion.builder()
                .id(1L).component(motorComp).versionNumber("2.0")
                .status(VersionStatus.APPROVED).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        controllerV3 = ComponentVersion.builder()
                .id(2L).component(controllerComp).versionNumber("3.0")
                .status(VersionStatus.APPROVED).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        firmwareV1 = ComponentVersion.builder()
                .id(3L).component(firmwareComp).versionNumber("1.0")
                .status(VersionStatus.APPROVED).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── createDependency ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createDependency: valid A→B — success")
    void createDependency_validEdge_success() {
        // Arrange: Motor v2 → Controller v3 (no existing deps from controller)
        CreateDependencyRequest request = new CreateDependencyRequest();
        request.setSourceVersionId(1L);
        request.setTargetVersionId(2L);
        request.setDependencyType(DependencyType.REQUIRES);
        request.setCreatedById(1L);

        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);
        when(versionService.findVersionEntityById(2L)).thenReturn(controllerV3);
        when(dependencyRepository.existsBySourceVersionAndTargetVersion(motorV2, controllerV3)).thenReturn(false);
        // DFS: starting from controllerV3 (id=2), no outgoing deps → no cycle
        when(dependencyRepository.findBySourceVersionId(2L)).thenReturn(Collections.emptyList());
        when(userService.findUserEntityById(1L)).thenReturn(testUser);

        Dependency savedDep = Dependency.builder()
                .id(100L).sourceVersion(motorV2).targetVersion(controllerV3)
                .dependencyType(DependencyType.REQUIRES).createdAt(LocalDateTime.now())
                .build();
        when(dependencyRepository.save(any(Dependency.class))).thenReturn(savedDep);

        // Act
        DependencyResponse response = dependencyService.createDependency(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getSourceVersionId()).isEqualTo(1L);
        assertThat(response.getTargetVersionId()).isEqualTo(2L);
        verify(dependencyRepository).save(any(Dependency.class));
    }

    @Test
    @DisplayName("createDependency: self-dependency — throws IllegalArgumentException")
    void createDependency_selfDependency_throwsIllegalArgumentException() {
        // Arrange: Motor v2 → Motor v2 (same version, same id)
        CreateDependencyRequest request = new CreateDependencyRequest();
        request.setSourceVersionId(1L);
        request.setTargetVersionId(1L);
        request.setDependencyType(DependencyType.REQUIRES);
        request.setCreatedById(1L);

        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);

        // Act & Assert
        assertThatThrownBy(() -> dependencyService.createDependency(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot depend on itself");

        verify(dependencyRepository, never()).save(any());
    }

    @Test
    @DisplayName("createDependency: duplicate dependency — throws IllegalArgumentException")
    void createDependency_duplicateDependency_throwsIllegalArgumentException() {
        // Arrange: Motor v2 → Controller v3 already exists
        CreateDependencyRequest request = new CreateDependencyRequest();
        request.setSourceVersionId(1L);
        request.setTargetVersionId(2L);
        request.setDependencyType(DependencyType.REQUIRES);
        request.setCreatedById(1L);

        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);
        when(versionService.findVersionEntityById(2L)).thenReturn(controllerV3);
        when(dependencyRepository.existsBySourceVersionAndTargetVersion(motorV2, controllerV3)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> dependencyService.createDependency(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already exists");

        verify(dependencyRepository, never()).save(any());
    }

    // ─── wouldCreateCycle — DFS algorithm ─────────────────────────────────────

    @Test
    @DisplayName("wouldCreateCycle: A→B linear chain — no cycle, returns false")
    void wouldCreateCycle_linearChain_returnsFalse() {
        // Existing: Motor(1) → Controller(2)
        // Adding: Controller(2) → Firmware(3) — safe (linear chain)
        // DFS from Firmware(3): no outgoing deps → can't reach Controller(2)
        when(dependencyRepository.findBySourceVersionId(3L)).thenReturn(Collections.emptyList());

        boolean result = dependencyService.wouldCreateCycle(2L, 3L); // sourceId=2, targetId=3

        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("wouldCreateCycle: A→B→C→A — cycle detected, returns true")
    void wouldCreateCycle_simpleCycle_A_B_C_A_returnsTrue() {
        // Existing edges: Motor(1)→Controller(2), Controller(2)→Firmware(3)
        // Attempting to add: Firmware(3)→Motor(1) — would close the cycle

        // DFS from Motor(1): follows Motor→Controller→Firmware→Motor (goalId=3? No...)
        // wouldCreateCycle(sourceId=3, targetId=1)
        //   → canReach(startId=1, goalId=3)
        //   → 1 != 3, follow Motor's deps → Controller(2)
        //   → canReach(2, 3)
        //   → 2 != 3, follow Controller's deps → Firmware(3)
        //   → canReach(3, 3) → 3 == 3 → TRUE (cycle found)

        Dependency motorToController = Dependency.builder()
                .id(1L).sourceVersion(motorV2).targetVersion(controllerV3)
                .dependencyType(DependencyType.REQUIRES).createdAt(LocalDateTime.now()).build();
        Dependency controllerToFirmware = Dependency.builder()
                .id(2L).sourceVersion(controllerV3).targetVersion(firmwareV1)
                .dependencyType(DependencyType.REQUIRES).createdAt(LocalDateTime.now()).build();

        // DFS from targetId=1 (Motor), trying to reach sourceId=3 (Firmware)
        when(dependencyRepository.findBySourceVersionId(1L)).thenReturn(List.of(motorToController));
        when(dependencyRepository.findBySourceVersionId(2L)).thenReturn(List.of(controllerToFirmware));
        // The DFS detects cycle at canReach(3, 3) == true before querying Firmware's deps,
        // but we set this up defensively. Use lenient() to suppress UnnecessaryStubbingException.
        lenient().when(dependencyRepository.findBySourceVersionId(3L)).thenReturn(Collections.emptyList());

        // Adding Firmware(3) → Motor(1): sourceId=3, targetId=1
        boolean result = dependencyService.wouldCreateCycle(3L, 1L);

        assertThat(result).isTrue();
    }


    @Test
    @DisplayName("createDependency: circular dependency C→A where A→B→C — throws CircularDependencyException")
    void createDependency_circularDependency_throwsCircularDependencyException() {
        // Existing: Motor(1)→Controller(2), Controller(2)→Firmware(3)
        // Attempting to add: Firmware(3)→Motor(1) — circular!

        CreateDependencyRequest request = new CreateDependencyRequest();
        request.setSourceVersionId(3L); // Firmware
        request.setTargetVersionId(1L); // Motor
        request.setDependencyType(DependencyType.REQUIRES);
        request.setCreatedById(1L);

        when(versionService.findVersionEntityById(3L)).thenReturn(firmwareV1);
        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);
        when(dependencyRepository.existsBySourceVersionAndTargetVersion(firmwareV1, motorV2)).thenReturn(false);

        // Set up the DFS: starting at targetId=1 (Motor), can we reach sourceId=3 (Firmware)?
        Dependency motorToController = Dependency.builder()
                .id(1L).sourceVersion(motorV2).targetVersion(controllerV3)
                .dependencyType(DependencyType.REQUIRES).createdAt(LocalDateTime.now()).build();
        Dependency controllerToFirmware = Dependency.builder()
                .id(2L).sourceVersion(controllerV3).targetVersion(firmwareV1)
                .dependencyType(DependencyType.REQUIRES).createdAt(LocalDateTime.now()).build();

        when(dependencyRepository.findBySourceVersionId(1L)).thenReturn(List.of(motorToController));
        when(dependencyRepository.findBySourceVersionId(2L)).thenReturn(List.of(controllerToFirmware));

        // Act & Assert
        assertThatThrownBy(() -> dependencyService.createDependency(request))
                .isInstanceOf(CircularDependencyException.class)
                .hasMessageContaining("circular dependency");

        verify(dependencyRepository, never()).save(any());
    }
}
