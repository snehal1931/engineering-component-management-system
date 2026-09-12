package com.edcms.service;

import com.edcms.dto.request.CreateComponentRequest;
import com.edcms.dto.response.ComponentResponse;
import com.edcms.entity.Component;
import com.edcms.entity.MechanicalComponent;
import com.edcms.entity.SoftwareComponent;
import com.edcms.entity.User;
import com.edcms.enums.ComponentStatus;
import com.edcms.enums.ComponentType;
import com.edcms.enums.Role;
import com.edcms.exception.ComponentNotFoundException;
import com.edcms.exception.DuplicateComponentException;
import com.edcms.mapper.ComponentMapper;
import com.edcms.repository.ComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link ComponentService}.
 *
 * <p>All repository and service dependencies are mocked with Mockito.
 * No Spring context, no database — pure business logic testing.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ComponentService Unit Tests")
class ComponentServiceTest {

    @Mock
    private ComponentRepository componentRepository;

    @Mock
    private UserService userService;

    @Mock
    private ComponentMapper componentMapper;

    @InjectMocks
    private ComponentService componentService;

    private User testUser;
    private MechanicalComponent testMechanicalComponent;
    private SoftwareComponent testSoftwareComponent;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("engineer1")
                .email("eng@edcms.com")
                .passwordHash("hashed")
                .role(Role.ENGINEER)
                .build();

        testMechanicalComponent = MechanicalComponent.builder()
                .id(1L)
                .componentCode("MECH-MOTOR-001")
                .name("High-Torque Brushless Motor")
                .description("48V brushless motor for drive system")
                .manufacturer("Bosch")
                .status(ComponentStatus.ACTIVE)
                .material("Aluminium")
                .weightKg(new BigDecimal("2.5"))
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testSoftwareComponent = SoftwareComponent.builder()
                .id(2L)
                .componentCode("SW-FW-CTRL-001")
                .name("Motor Controller Firmware")
                .status(ComponentStatus.ACTIVE)
                .programmingLang("C")
                .licenseType("Proprietary")
                .createdBy(testUser)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    // ─── createComponent ─────────────────────────────────────────────────────

    @Test
    @DisplayName("createComponent: MECHANICAL — success")
    void createComponent_mechanical_success() {
        // Arrange
        CreateComponentRequest request = new CreateComponentRequest();
        request.setComponentCode("MECH-MOTOR-001");
        request.setName("High-Torque Brushless Motor");
        request.setComponentType(ComponentType.MECHANICAL);
        request.setCreatedById(1L);
        request.setMaterial("Aluminium");
        request.setWeightKg(new BigDecimal("2.5"));

        when(componentRepository.existsByComponentCode("MECH-MOTOR-001")).thenReturn(false);
        when(userService.findUserEntityById(1L)).thenReturn(testUser);
        when(componentRepository.save(any(Component.class))).thenReturn(testMechanicalComponent);
        ComponentResponse expectedResponse = buildMechanicalResponse();
        when(componentMapper.toResponse(testMechanicalComponent)).thenReturn(expectedResponse);

        // Act
        ComponentResponse response = componentService.createComponent(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getComponentCode()).isEqualTo("MECH-MOTOR-001");
        assertThat(response.getComponentType()).isEqualTo(ComponentType.MECHANICAL);
        verify(componentRepository).save(any(MechanicalComponent.class));
    }

    @Test
    @DisplayName("createComponent: SOFTWARE — success")
    void createComponent_software_success() {
        // Arrange
        CreateComponentRequest request = new CreateComponentRequest();
        request.setComponentCode("SW-FW-CTRL-001");
        request.setName("Motor Controller Firmware");
        request.setComponentType(ComponentType.SOFTWARE);
        request.setCreatedById(1L);
        request.setProgrammingLang("C");
        request.setLicenseType("Proprietary");

        when(componentRepository.existsByComponentCode("SW-FW-CTRL-001")).thenReturn(false);
        when(userService.findUserEntityById(1L)).thenReturn(testUser);
        when(componentRepository.save(any(Component.class))).thenReturn(testSoftwareComponent);
        ComponentResponse expectedResponse = buildSoftwareResponse();
        when(componentMapper.toResponse(testSoftwareComponent)).thenReturn(expectedResponse);

        // Act
        ComponentResponse response = componentService.createComponent(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getComponentCode()).isEqualTo("SW-FW-CTRL-001");
        assertThat(response.getComponentType()).isEqualTo(ComponentType.SOFTWARE);
        verify(componentRepository).save(any(SoftwareComponent.class));
    }

    @Test
    @DisplayName("createComponent: duplicate code — throws DuplicateComponentException")
    void createComponent_duplicateCode_throwsDuplicateComponentException() {
        // Arrange
        CreateComponentRequest request = new CreateComponentRequest();
        request.setComponentCode("MECH-MOTOR-001");
        request.setName("Another Motor");
        request.setComponentType(ComponentType.MECHANICAL);
        request.setCreatedById(1L);

        when(componentRepository.existsByComponentCode("MECH-MOTOR-001")).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> componentService.createComponent(request))
                .isInstanceOf(DuplicateComponentException.class);

        verify(componentRepository, never()).save(any());
    }

    // ─── getComponentById ─────────────────────────────────────────────────────

    @Test
    @DisplayName("getComponentById: found — returns response")
    void getComponentById_found_returnsResponse() {
        // Arrange
        when(componentRepository.findById(1L)).thenReturn(Optional.of(testMechanicalComponent));
        ComponentResponse expectedResponse = buildMechanicalResponse();
        when(componentMapper.toResponse(testMechanicalComponent)).thenReturn(expectedResponse);

        // Act
        ComponentResponse response = componentService.getComponentById(1L);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getComponentById: not found — throws ComponentNotFoundException")
    void getComponentById_notFound_throwsComponentNotFoundException() {
        // Arrange
        when(componentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> componentService.getComponentById(99L))
                .isInstanceOf(ComponentNotFoundException.class);
    }

    // ─── updateComponent ─────────────────────────────────────────────────────

    @Test
    @DisplayName("updateComponent: success — updates name, description, manufacturer")
    void updateComponent_success_updatesNameDescriptionManufacturer() {
        // Arrange
        CreateComponentRequest updateRequest = new CreateComponentRequest();
        updateRequest.setName("Updated Motor Name");
        updateRequest.setDescription("Updated description");
        updateRequest.setManufacturer("Siemens");
        updateRequest.setComponentCode("MECH-MOTOR-001"); // needed for validation
        updateRequest.setComponentType(ComponentType.MECHANICAL);
        updateRequest.setCreatedById(1L);

        when(componentRepository.findById(1L)).thenReturn(Optional.of(testMechanicalComponent));
        when(componentRepository.save(testMechanicalComponent)).thenReturn(testMechanicalComponent);
        ComponentResponse expectedResponse = buildMechanicalResponse();
        when(componentMapper.toResponse(testMechanicalComponent)).thenReturn(expectedResponse);

        // Act
        ComponentResponse response = componentService.updateComponent(1L, updateRequest);

        // Assert
        assertThat(response).isNotNull();
        // Verify the entity was mutated before saving
        assertThat(testMechanicalComponent.getName()).isEqualTo("Updated Motor Name");
        assertThat(testMechanicalComponent.getDescription()).isEqualTo("Updated description");
        assertThat(testMechanicalComponent.getManufacturer()).isEqualTo("Siemens");
        verify(componentRepository).save(testMechanicalComponent);
    }

    @Test
    @DisplayName("updateComponent: component not found — throws ComponentNotFoundException")
    void updateComponent_notFound_throwsComponentNotFoundException() {
        // Arrange
        CreateComponentRequest updateRequest = new CreateComponentRequest();
        updateRequest.setName("Some Name");
        updateRequest.setComponentCode("MECH-MOTOR-001");
        updateRequest.setComponentType(ComponentType.MECHANICAL);
        updateRequest.setCreatedById(1L);

        when(componentRepository.findById(99L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> componentService.updateComponent(99L, updateRequest))
                .isInstanceOf(ComponentNotFoundException.class);
        verify(componentRepository, never()).save(any());
    }

    // ─── deprecateComponent ──────────────────────────────────────────────────

    @Test
    @DisplayName("deprecateComponent: success — status set to DEPRECATED")
    void deprecateComponent_success_setsStatusDeprecated() {
        // Arrange
        when(componentRepository.findById(1L)).thenReturn(Optional.of(testMechanicalComponent));
        when(componentRepository.save(testMechanicalComponent)).thenReturn(testMechanicalComponent);

        // Act
        componentService.deprecateComponent(1L);

        // Assert
        assertThat(testMechanicalComponent.getStatus()).isEqualTo(ComponentStatus.DEPRECATED);
        verify(componentRepository).save(testMechanicalComponent);
    }

    // ─── Helper builders ─────────────────────────────────────────────────────

    private ComponentResponse buildMechanicalResponse() {
        return ComponentResponse.builder()
                .id(1L)
                .componentCode("MECH-MOTOR-001")
                .name("High-Torque Brushless Motor")
                .componentType(ComponentType.MECHANICAL)
                .status(ComponentStatus.ACTIVE)
                .material("Aluminium")
                .weightKg(new BigDecimal("2.5"))
                .build();
    }

    private ComponentResponse buildSoftwareResponse() {
        return ComponentResponse.builder()
                .id(2L)
                .componentCode("SW-FW-CTRL-001")
                .name("Motor Controller Firmware")
                .componentType(ComponentType.SOFTWARE)
                .status(ComponentStatus.ACTIVE)
                .programmingLang("C")
                .licenseType("Proprietary")
                .build();
    }
}
