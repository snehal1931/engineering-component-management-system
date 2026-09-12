package com.edcms.service;

import com.edcms.dto.request.CreateComponentVersionRequest;
import com.edcms.dto.response.ComponentVersionResponse;
import com.edcms.entity.Component;
import com.edcms.entity.ComponentVersion;
import com.edcms.entity.MechanicalComponent;
import com.edcms.entity.User;
import com.edcms.enums.ComponentStatus;
import com.edcms.enums.Role;
import com.edcms.enums.VersionStatus;
import com.edcms.exception.ComponentVersionNotFoundException;
import com.edcms.exception.InvalidVersionTransitionException;
import com.edcms.mapper.ComponentVersionMapper;
import com.edcms.repository.ComponentVersionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ComponentVersionService}.
 *
 * <p>Focuses on the version state machine — the most critical business logic
 * in this service. Every valid and invalid transition is tested explicitly.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComponentVersionService Unit Tests")
class ComponentVersionServiceTest {

    @Mock
    private ComponentVersionRepository versionRepository;

    @Mock
    private ComponentService componentService;

    @Mock
    private UserService userService;

    @Mock
    private ComponentVersionMapper versionMapper;

    @InjectMocks
    private ComponentVersionService componentVersionService;

    private Component testComponent;
    private User testUser;
    private ComponentVersion draftVersion;
    private ComponentVersion approvedVersion;
    private ComponentVersion deprecatedVersion;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("admin1")
                .email("admin@edcms.com")
                .passwordHash("hashed")
                .role(Role.ADMIN)
                .build();

        testComponent = MechanicalComponent.builder()
                .id(1L)
                .componentCode("MECH-MOTOR-001")
                .name("High-Torque Brushless Motor")
                .status(ComponentStatus.ACTIVE)
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        draftVersion = ComponentVersion.builder()
                .id(10L)
                .component(testComponent)
                .versionNumber("2.0")
                .status(VersionStatus.DRAFT)
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        approvedVersion = ComponentVersion.builder()
                .id(11L)
                .component(testComponent)
                .versionNumber("1.5")
                .status(VersionStatus.APPROVED)
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        deprecatedVersion = ComponentVersion.builder()
                .id(12L)
                .component(testComponent)
                .versionNumber("1.0")
                .status(VersionStatus.DEPRECATED)
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── createVersion ────────────────────────────────────────────────────────

    @Test
    @DisplayName("createVersion: success — creates DRAFT version")
    void createVersion_success_createsDraftVersion() {
        // Arrange
        CreateComponentVersionRequest request = new CreateComponentVersionRequest();
        request.setVersionNumber("2.0");
        request.setReleaseNotes("Major torque upgrade");
        request.setCreatedById(1L);

        when(componentService.findComponentEntityById(1L)).thenReturn(testComponent);
        when(versionRepository.existsByComponentAndVersionNumber(testComponent, "2.0")).thenReturn(false);
        when(userService.findUserEntityById(1L)).thenReturn(testUser);
        when(versionRepository.save(any(ComponentVersion.class))).thenReturn(draftVersion);
        ComponentVersionResponse expectedResponse = buildVersionResponse(10L, "2.0", VersionStatus.DRAFT);
        when(versionMapper.toResponse(draftVersion)).thenReturn(expectedResponse);

        // Act
        ComponentVersionResponse response = componentVersionService.createVersion(1L, request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getVersionNumber()).isEqualTo("2.0");
        assertThat(response.getStatus()).isEqualTo(VersionStatus.DRAFT);
        verify(versionRepository).save(argThat(v -> v.getStatus() == VersionStatus.DRAFT));
    }

    @Test
    @DisplayName("createVersion: duplicate version number — throws IllegalArgumentException")
    void createVersion_duplicateVersionNumber_throwsIllegalArgumentException() {
        // Arrange
        CreateComponentVersionRequest request = new CreateComponentVersionRequest();
        request.setVersionNumber("2.0");
        request.setCreatedById(1L);

        when(componentService.findComponentEntityById(1L)).thenReturn(testComponent);
        when(versionRepository.existsByComponentAndVersionNumber(testComponent, "2.0")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> componentVersionService.createVersion(1L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("2.0")
                .hasMessageContaining("already exists");

        verify(versionRepository, never()).save(any());
    }

    // ─── approveVersion — state machine ───────────────────────────────────────

    @Test
    @DisplayName("approveVersion: DRAFT → APPROVED — valid transition")
    void approveVersion_draftToApproved_validTransition() {
        // Arrange
        when(versionRepository.findById(10L)).thenReturn(Optional.of(draftVersion));
        when(versionRepository.save(draftVersion)).thenReturn(draftVersion);
        ComponentVersionResponse expectedResponse = buildVersionResponse(10L, "2.0", VersionStatus.APPROVED);
        when(versionMapper.toResponse(draftVersion)).thenReturn(expectedResponse);

        // Act
        ComponentVersionResponse response = componentVersionService.approveVersion(10L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(VersionStatus.APPROVED);
        assertThat(draftVersion.getStatus()).isEqualTo(VersionStatus.APPROVED);
    }

    @Test
    @DisplayName("approveVersion: APPROVED → APPROVED — invalid, throws InvalidVersionTransitionException")
    void approveVersion_approvedToApproved_throwsInvalidVersionTransitionException() {
        // Arrange
        when(versionRepository.findById(11L)).thenReturn(Optional.of(approvedVersion));

        // Act & Assert
        assertThatThrownBy(() -> componentVersionService.approveVersion(11L))
                .isInstanceOf(InvalidVersionTransitionException.class);
    }

    @Test
    @DisplayName("approveVersion: DEPRECATED → APPROVED — invalid, throws InvalidVersionTransitionException")
    void approveVersion_deprecatedToApproved_throwsInvalidVersionTransitionException() {
        // Arrange
        when(versionRepository.findById(12L)).thenReturn(Optional.of(deprecatedVersion));

        // Act & Assert
        assertThatThrownBy(() -> componentVersionService.approveVersion(12L))
                .isInstanceOf(InvalidVersionTransitionException.class);

        verify(versionRepository, never()).save(any());
    }

    // ─── deprecateVersion — state machine ─────────────────────────────────────

    @Test
    @DisplayName("deprecateVersion: APPROVED → DEPRECATED — valid transition")
    void deprecateVersion_approvedToDeprecated_validTransition() {
        // Arrange
        when(versionRepository.findById(11L)).thenReturn(Optional.of(approvedVersion));
        when(versionRepository.save(approvedVersion)).thenReturn(approvedVersion);
        ComponentVersionResponse expectedResponse = buildVersionResponse(11L, "1.5", VersionStatus.DEPRECATED);
        when(versionMapper.toResponse(approvedVersion)).thenReturn(expectedResponse);

        // Act
        ComponentVersionResponse response = componentVersionService.deprecateVersion(11L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(VersionStatus.DEPRECATED);
        assertThat(approvedVersion.getStatus()).isEqualTo(VersionStatus.DEPRECATED);
    }

    @Test
    @DisplayName("deprecateVersion: DRAFT → DEPRECATED — valid transition (skip APPROVED)")
    void deprecateVersion_draftToDeprecated_validTransition() {
        // Arrange
        when(versionRepository.findById(10L)).thenReturn(Optional.of(draftVersion));
        when(versionRepository.save(draftVersion)).thenReturn(draftVersion);
        ComponentVersionResponse expectedResponse = buildVersionResponse(10L, "2.0", VersionStatus.DEPRECATED);
        when(versionMapper.toResponse(draftVersion)).thenReturn(expectedResponse);

        // Act
        ComponentVersionResponse response = componentVersionService.deprecateVersion(10L);

        // Assert
        assertThat(response.getStatus()).isEqualTo(VersionStatus.DEPRECATED);
    }

    @Test
    @DisplayName("deprecateVersion: DEPRECATED → DEPRECATED — invalid, terminal state")
    void deprecateVersion_deprecatedToDeprecated_throwsInvalidVersionTransitionException() {
        // Arrange
        when(versionRepository.findById(12L)).thenReturn(Optional.of(deprecatedVersion));

        // Act & Assert
        assertThatThrownBy(() -> componentVersionService.deprecateVersion(12L))
                .isInstanceOf(InvalidVersionTransitionException.class);
    }

    // ─── validateTransition — exhaustive state machine test ───────────────────

    @Test
    @DisplayName("validateTransition: all valid transitions pass")
    void validateTransition_allValidTransitions_noException() {
        // DRAFT → APPROVED: valid
        assertThatNoException().isThrownBy(
                () -> componentVersionService.validateTransition(VersionStatus.DRAFT, VersionStatus.APPROVED));

        // DRAFT → DEPRECATED: valid
        assertThatNoException().isThrownBy(
                () -> componentVersionService.validateTransition(VersionStatus.DRAFT, VersionStatus.DEPRECATED));

        // APPROVED → DEPRECATED: valid
        assertThatNoException().isThrownBy(
                () -> componentVersionService.validateTransition(VersionStatus.APPROVED, VersionStatus.DEPRECATED));
    }

    @Test
    @DisplayName("validateTransition: APPROVED → DRAFT — forbidden")
    void validateTransition_approvedToDraft_throwsInvalidVersionTransitionException() {
        assertThatThrownBy(
                () -> componentVersionService.validateTransition(VersionStatus.APPROVED, VersionStatus.DRAFT))
                .isInstanceOf(InvalidVersionTransitionException.class);
    }

    @Test
    @DisplayName("validateTransition: DEPRECATED → DRAFT — forbidden (terminal state)")
    void validateTransition_deprecatedToDraft_throwsInvalidVersionTransitionException() {
        assertThatThrownBy(
                () -> componentVersionService.validateTransition(VersionStatus.DEPRECATED, VersionStatus.DRAFT))
                .isInstanceOf(InvalidVersionTransitionException.class);
    }

    @Test
    @DisplayName("validateTransition: DEPRECATED → APPROVED — forbidden (terminal state)")
    void validateTransition_deprecatedToApproved_throwsInvalidVersionTransitionException() {
        assertThatThrownBy(
                () -> componentVersionService.validateTransition(VersionStatus.DEPRECATED, VersionStatus.APPROVED))
                .isInstanceOf(InvalidVersionTransitionException.class);
    }

    // ─── getVersionById ───────────────────────────────────────────────────────

    @Test
    @DisplayName("getVersionById: not found — throws ComponentVersionNotFoundException")
    void getVersionById_notFound_throwsComponentVersionNotFoundException() {
        when(versionRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> componentVersionService.getVersionById(999L))
                .isInstanceOf(ComponentVersionNotFoundException.class);
    }

    // ─── Helper builders ─────────────────────────────────────────────────────

    private ComponentVersionResponse buildVersionResponse(Long id, String version, VersionStatus status) {
        return ComponentVersionResponse.builder()
                .id(id)
                .componentId(1L)
                .versionNumber(version)
                .status(status)
                .build();
    }
}
