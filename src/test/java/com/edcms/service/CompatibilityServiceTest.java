package com.edcms.service;

import com.edcms.checker.CompatibilityChecker;
import com.edcms.dto.request.CompatibilityCheckRequest;
import com.edcms.dto.request.CreateCompatibilityRuleRequest;
import com.edcms.dto.response.CompatibilityCheckResponse;
import com.edcms.dto.response.CompatibilityRuleResponse;
import com.edcms.entity.CompatibilityRule;
import com.edcms.entity.ComponentVersion;
import com.edcms.entity.MechanicalComponent;
import com.edcms.entity.SoftwareComponent;
import com.edcms.entity.User;
import com.edcms.enums.ComponentStatus;
import com.edcms.enums.Role;
import com.edcms.enums.VersionStatus;
import com.edcms.exception.CompatibilityException;
import com.edcms.repository.CompatibilityRuleRepository;
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
 * Unit tests for {@link CompatibilityService}.
 *
 * <p>Key scenarios:
 * <ul>
 *   <li>Motor v2.0 + Controller v2.0 → incompatible (rule exists, isCompatible=false)</li>
 *   <li>Motor v2.0 + Controller v3.0 → compatible (rule exists, isCompatible=true)</li>
 *   <li>No rule exists → assume compatible (lenient default)</li>
 * </ul>
 *
 * <p>The {@link CompatibilityChecker} is mocked to isolate service logic from
 * checker implementation details. Checker is separately tested in {@code DefaultCompatibilityCheckerTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CompatibilityService Unit Tests")
class CompatibilityServiceTest {

    @Mock
    private CompatibilityRuleRepository compatibilityRuleRepository;

    @Mock
    private CompatibilityChecker compatibilityChecker;

    @Mock
    private ComponentVersionService versionService;

    @Mock
    private UserService userService;

    @InjectMocks
    private CompatibilityService compatibilityService;

    private User testUser;
    private ComponentVersion motorV2;    // id=1
    private ComponentVersion ctrlV2;     // id=2
    private ComponentVersion ctrlV3;     // id=3

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L).username("admin").email("a@a.com").passwordHash("h").role(Role.ADMIN)
                .build();

        MechanicalComponent motorComp = MechanicalComponent.builder()
                .id(10L).componentCode("MECH-MOTOR-001").name("Motor")
                .status(ComponentStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        SoftwareComponent ctrlComp = SoftwareComponent.builder()
                .id(11L).componentCode("SW-CTRL-001").name("Controller")
                .status(ComponentStatus.ACTIVE).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        motorV2 = ComponentVersion.builder()
                .id(1L).component(motorComp).versionNumber("2.0")
                .status(VersionStatus.APPROVED).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        ctrlV2 = ComponentVersion.builder()
                .id(2L).component(ctrlComp).versionNumber("2.0")
                .status(VersionStatus.APPROVED).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();

        ctrlV3 = ComponentVersion.builder()
                .id(3L).component(ctrlComp).versionNumber("3.0")
                .status(VersionStatus.APPROVED).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── createRule ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("createRule: Motor v2 incompatible with Controller v2 — created successfully")
    void createRule_motorV2IncompatibleWithCtrlV2_success() {
        // Arrange
        CreateCompatibilityRuleRequest request = new CreateCompatibilityRuleRequest();
        request.setVersionAId(1L);
        request.setVersionBId(2L);
        request.setIsCompatible(false);
        request.setRuleDescription("Motor v2.0 requires CAN 2.0 interface, Controller v2.0 uses CAN 1.0");
        request.setCreatedById(1L);

        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);
        when(versionService.findVersionEntityById(2L)).thenReturn(ctrlV2);
        when(compatibilityRuleRepository.existsByVersionPair(1L, 2L)).thenReturn(false);
        when(userService.findUserEntityById(1L)).thenReturn(testUser);

        CompatibilityRule savedRule = CompatibilityRule.builder()
                .id(100L)
                .componentVersionA(motorV2)   // smaller id = A
                .componentVersionB(ctrlV2)
                .isCompatible(false)
                .ruleDescription("Motor v2.0 requires CAN 2.0 interface")
                .createdAt(LocalDateTime.now())
                .build();
        when(compatibilityRuleRepository.save(any(CompatibilityRule.class))).thenReturn(savedRule);

        // Act
        CompatibilityRuleResponse response = compatibilityService.createRule(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.isCompatible()).isFalse();
        verify(compatibilityRuleRepository).save(any(CompatibilityRule.class));
    }

    @Test
    @DisplayName("createRule: same version for A and B — throws IllegalArgumentException")
    void createRule_sameVersionForBothSides_throwsIllegalArgumentException() {
        // Arrange
        CreateCompatibilityRuleRequest request = new CreateCompatibilityRuleRequest();
        request.setVersionAId(1L);
        request.setVersionBId(1L);
        request.setIsCompatible(true);
        request.setRuleDescription("Self rule");
        request.setCreatedById(1L);

        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);

        // Act & Assert
        assertThatThrownBy(() -> compatibilityService.createRule(request))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("createRule: duplicate rule for same pair — throws CompatibilityException")
    void createRule_duplicateRuleForSamePair_throwsCompatibilityException() {
        // Arrange
        CreateCompatibilityRuleRequest request = new CreateCompatibilityRuleRequest();
        request.setVersionAId(1L);
        request.setVersionBId(2L);
        request.setIsCompatible(false);
        request.setRuleDescription("Duplicate rule");
        request.setCreatedById(1L);

        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);
        when(versionService.findVersionEntityById(2L)).thenReturn(ctrlV2);
        when(compatibilityRuleRepository.existsByVersionPair(1L, 2L)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> compatibilityService.createRule(request))
                .isInstanceOf(CompatibilityException.class);
    }

    // ─── checkVersions ────────────────────────────────────────────────────────

    @Test
    @DisplayName("checkVersions: Motor v2 + Controller v3 — no violations (compatible)")
    void checkVersions_motorV2WithCtrlV3_compatible() {
        // Arrange: checker returns all-compatible result
        CompatibilityCheckRequest request = new CompatibilityCheckRequest();
        request.setComponentVersionIds(List.of(1L, 3L));

        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);
        when(versionService.findVersionEntityById(3L)).thenReturn(ctrlV3);

        CompatibilityCheckResponse compatibleResponse = CompatibilityCheckResponse.builder()
                .allCompatible(true)
                .message("All specified component versions are compatible.")
                .violations(Collections.emptyList())
                .build();
        when(compatibilityChecker.checkAll(List.of(motorV2, ctrlV3))).thenReturn(compatibleResponse);

        // Act
        CompatibilityCheckResponse response = compatibilityService.checkVersions(request);

        // Assert
        assertThat(response.isAllCompatible()).isTrue();
        assertThat(response.getViolations()).isEmpty();
    }

    @Test
    @DisplayName("checkVersions: Motor v2 + Controller v2 — incompatible (rule says false)")
    void checkVersions_motorV2WithCtrlV2_incompatible() {
        // Arrange: checker returns incompatible result
        CompatibilityCheckRequest request = new CompatibilityCheckRequest();
        request.setComponentVersionIds(List.of(1L, 2L));

        when(versionService.findVersionEntityById(1L)).thenReturn(motorV2);
        when(versionService.findVersionEntityById(2L)).thenReturn(ctrlV2);

        CompatibilityCheckResponse.IncompatiblePair violation = CompatibilityCheckResponse.IncompatiblePair.builder()
                .versionAId(1L).versionADescription("MECH-MOTOR-001 v2.0")
                .versionBId(2L).versionBDescription("SW-CTRL-001 v2.0")
                .reason("Motor v2.0 requires CAN 2.0 interface, Controller v2.0 uses CAN 1.0")
                .build();
        CompatibilityCheckResponse incompatibleResponse = CompatibilityCheckResponse.builder()
                .allCompatible(false)
                .message("1 incompatibility violation(s) found.")
                .violations(List.of(violation))
                .build();
        when(compatibilityChecker.checkAll(List.of(motorV2, ctrlV2))).thenReturn(incompatibleResponse);

        // Act
        CompatibilityCheckResponse response = compatibilityService.checkVersions(request);

        // Assert
        assertThat(response.isAllCompatible()).isFalse();
        assertThat(response.getViolations()).hasSize(1);
        assertThat(response.getViolations().get(0).getReason()).contains("CAN");
    }

    // ─── validateBeforeAdding ─────────────────────────────────────────────────

    @Test
    @DisplayName("validateBeforeAdding: no existing versions — skips check (empty assembly)")
    void validateBeforeAdding_noExistingVersions_noExceptionThrown() {
        // Act & Assert: should not throw — nothing to check against
        assertThatNoException().isThrownBy(
                () -> compatibilityService.validateBeforeAdding(motorV2, Collections.emptyList()));

        verify(compatibilityChecker, never()).check(any(), any());
    }

    @Test
    @DisplayName("validateBeforeAdding: compatible new version — no exception")
    void validateBeforeAdding_compatibleNewVersion_noException() {
        // Arrange: checker says motorV2 is compatible with ctrlV3
        CompatibilityCheckResponse compatibleResponse = CompatibilityCheckResponse.builder()
                .allCompatible(true)
                .message("Compatible")
                .violations(Collections.emptyList())
                .build();
        when(compatibilityChecker.check(motorV2, List.of(ctrlV3))).thenReturn(compatibleResponse);

        // Act & Assert
        assertThatNoException().isThrownBy(
                () -> compatibilityService.validateBeforeAdding(motorV2, List.of(ctrlV3)));
    }

    @Test
    @DisplayName("validateBeforeAdding: incompatible new version — throws CompatibilityException")
    void validateBeforeAdding_incompatibleNewVersion_throwsCompatibilityException() {
        // Arrange: checker says motorV2 is NOT compatible with ctrlV2
        CompatibilityCheckResponse.IncompatiblePair violation = CompatibilityCheckResponse.IncompatiblePair.builder()
                .versionAId(1L).versionADescription("MECH-MOTOR-001 v2.0")
                .versionBId(2L).versionBDescription("SW-CTRL-001 v2.0")
                .reason("Requires CAN 2.0, Controller v2.0 uses CAN 1.0")
                .build();
        CompatibilityCheckResponse incompatibleResponse = CompatibilityCheckResponse.builder()
                .allCompatible(false)
                .message("Incompatible")
                .violations(List.of(violation))
                .build();
        when(compatibilityChecker.check(motorV2, List.of(ctrlV2))).thenReturn(incompatibleResponse);

        // Act & Assert
        assertThatThrownBy(() -> compatibilityService.validateBeforeAdding(motorV2, List.of(ctrlV2)))
                .isInstanceOf(CompatibilityException.class)
                .hasMessageContaining("compatibility violation");
    }
}
