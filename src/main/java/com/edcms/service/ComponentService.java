package com.edcms.service;

import com.edcms.dto.request.CreateComponentRequest;
import com.edcms.dto.response.ComponentResponse;
import com.edcms.entity.*;
import com.edcms.enums.ComponentStatus;
import com.edcms.enums.ComponentType;
import com.edcms.exception.ComponentNotFoundException;
import com.edcms.exception.DuplicateComponentException;
import com.edcms.mapper.ComponentMapper;
import com.edcms.repository.ComponentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for Engineering Component management.
 *
 * <p><b>Factory-like instantiation:</b><br>
 * The {@link #buildComponent(CreateComponentRequest, User)} method acts as a
 * simple factory — it reads the {@code componentType} from the request and
 * instantiates the correct entity subclass. This is a pragmatic alternative
 * to a formal Factory pattern for three known types.
 *
 * <p>If many more component types were expected, a proper Factory or
 * Abstract Factory pattern would be justified to avoid the growing if/else chain.
 * For three types, this approach is clear and maintainable.
 *
 * <p><b>OCP consideration:</b> Adding a new component type requires:
 * <ol>
 *   <li>A new enum value in ComponentType.</li>
 *   <li>A new entity subclass.</li>
 *   <li>A new branch in {@code buildComponent} and {@code ComponentMapper}.</li>
 * </ol>
 * This is a deliberate trade-off — a pure Factory pattern would eliminate the
 * third point at the cost of more abstraction complexity.
 */
@Service
@RequiredArgsConstructor
public class ComponentService {

    private static final Logger log = LoggerFactory.getLogger(ComponentService.class);

    private final ComponentRepository componentRepository;
    private final UserService userService;
    private final ComponentMapper componentMapper;

    @Transactional
    public ComponentResponse createComponent(CreateComponentRequest request) {
        log.info("Creating component: code='{}', type={}", request.getComponentCode(), request.getComponentType());

        // Enforce globally unique component code
        if (componentRepository.existsByComponentCode(request.getComponentCode())) {
            throw new DuplicateComponentException(request.getComponentCode());
        }

        User createdBy = userService.findUserEntityById(request.getCreatedById());
        Component component = buildComponent(request, createdBy);

        Component saved = componentRepository.save(component);
        log.info("Component '{}' (id: {}) created successfully", saved.getComponentCode(), saved.getId());
        return componentMapper.toResponse(saved);

    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> getAllComponents() {
        return componentRepository.findAll().stream()
                .map(componentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ComponentResponse> getComponentsByType(ComponentType type) {
        return componentRepository.findByComponentType(type).stream()
                .map(componentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ComponentResponse getComponentById(Long id) {
        return componentMapper.toResponse(findComponentEntityById(id));
    }

    @Transactional
    public void deprecateComponent(Long id) {
        Component component = findComponentEntityById(id);
        component.setStatus(ComponentStatus.DEPRECATED);
        componentRepository.save(component);
        log.warn("Component id: {} ('{}') has been DEPRECATED", id, component.getComponentCode());
    }

    /**
     * Update mutable fields of a component: name, description, manufacturer.
     *
     * <p><b>Intentionally immutable fields:</b>
     * <ul>
     *   <li>{@code componentCode} — referenced in engineering drawings and documents;
     *       changing it would break traceability. To rename, deprecate and create new.</li>
     *   <li>{@code componentType} — changing type (e.g., MECHANICAL → ELECTRICAL)
     *       would require deleting/creating rows across separate JPA-inherited tables.</li>
     * </ul>
     */
    @Transactional
    public ComponentResponse updateComponent(Long id, CreateComponentRequest request) {
        Component component = findComponentEntityById(id);
        component.setName(request.getName());
        component.setDescription(request.getDescription());
        component.setManufacturer(request.getManufacturer());
        Component saved = componentRepository.save(component);
        log.info("Component id: {} ('{}') updated", id, saved.getComponentCode());
        return componentMapper.toResponse(saved);
    }

    /** Internal entity loader used by ComponentVersionService. */
    @Transactional(readOnly = true)
    public Component findComponentEntityById(Long id) {
        return componentRepository.findById(id)
                .orElseThrow(() -> new ComponentNotFoundException(id));
    }

    /**
     * Instantiate the correct Component subclass based on {@code componentType}.
     *
     * <p>This is where the OOP inheritance hierarchy pays off — all three subtypes
     * share the same base class, so we can set common fields once and then
     * conditionally set subtype-specific fields.
     */
    private Component buildComponent(CreateComponentRequest req, User createdBy) {
        return switch (req.getComponentType()) {
            case MECHANICAL -> MechanicalComponent.builder()
                    .componentCode(req.getComponentCode())
                    .name(req.getName())
                    .description(req.getDescription())
                    .manufacturer(req.getManufacturer())
                    .status(ComponentStatus.ACTIVE)
                    .createdBy(createdBy)
                    .material(req.getMaterial())
                    .weightKg(req.getWeightKg())
                    .build();

            case ELECTRICAL -> ElectricalComponent.builder()
                    .componentCode(req.getComponentCode())
                    .name(req.getName())
                    .description(req.getDescription())
                    .manufacturer(req.getManufacturer())
                    .status(ComponentStatus.ACTIVE)
                    .createdBy(createdBy)
                    .voltageRating(req.getVoltageRating())
                    .currentRating(req.getCurrentRating())
                    .powerRatingW(req.getPowerRatingW())
                    .build();

            case SOFTWARE -> SoftwareComponent.builder()
                    .componentCode(req.getComponentCode())
                    .name(req.getName())
                    .description(req.getDescription())
                    .manufacturer(req.getManufacturer())
                    .status(ComponentStatus.ACTIVE)
                    .createdBy(createdBy)
                    .programmingLang(req.getProgrammingLang())
                    .licenseType(req.getLicenseType())
                    .build();
        };
    }
}
