package com.edcms.service;

import com.edcms.dto.response.ImpactAnalysisResponse;
import com.edcms.entity.AssemblyComponent;
import com.edcms.entity.ComponentVersion;
import com.edcms.repository.AssemblyComponentRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for impact analysis — determines which assemblies are affected
 * when a component version is deprecated or changed.
 *
 * <p><b>Business problem solved:</b><br>
 * An engineer wants to deprecate "Battery v1.0" but needs to know:
 * "Which assemblies currently use Battery v1.0? What will break?"
 *
 * <p>This service provides the answer by querying the {@code assembly_components}
 * join table to find all assemblies that reference the given version.
 *
 * <p><b>Single Responsibility Principle:</b><br>
 * Impact analysis is a distinct business capability — not a sub-feature of
 * assembly management or versioning. It gets its own service class with its
 * own focused responsibility.
 */
@Service
@RequiredArgsConstructor
public class ImpactAnalysisService {

    private static final Logger log = LoggerFactory.getLogger(ImpactAnalysisService.class);

    private final AssemblyComponentRepository assemblyComponentRepository;
    private final ComponentVersionService versionService;
    private final ComponentService componentService;

    /**
     * Find all assemblies currently using a specific component version.
     *
     * <p>Typical use case: before deprecating a version, run this to see the impact.
     *
     * <p>Example response when Battery v1.0 is used in 3 assemblies:
     * <pre>
     * {
     *   "componentVersionId": 5,
     *   "versionNumber": "1.0",
     *   "componentCode": "ELEC-BATT-001",
     *   "totalAffectedAssemblies": 3,
     *   "affectedAssemblies": [
     *     { "assemblyId": 1, "assemblyName": "Powertrain Assembly", ... },
     *     { "assemblyId": 2, "assemblyName": "Test Assembly", ... },
     *     { "assemblyId": 3, "assemblyName": "Prototype Assembly", ... }
     *   ]
     * }
     * </pre>
     */
    @Transactional(readOnly = true)
    public ImpactAnalysisResponse analyzeVersionImpact(Long versionId) {
        ComponentVersion version = versionService.findVersionEntityById(versionId);

        List<AssemblyComponent> usages = assemblyComponentRepository.findByComponentVersionId(versionId);

        log.info("Impact analysis for version '{}' (id: {}): found {} affected assemblies",
                version.getVersionNumber(), versionId, usages.size());

        List<ImpactAnalysisResponse.AffectedAssemblyInfo> affectedList = usages.stream()
                .map(ac -> ImpactAnalysisResponse.AffectedAssemblyInfo.builder()
                        .assemblyId(ac.getAssembly().getId())
                        .assemblyName(ac.getAssembly().getName())
                        .projectId(ac.getAssembly().getProject().getId())
                        .projectName(ac.getAssembly().getProject().getName())
                        .versionAddedAt(ac.getAddedAt())
                        .build())
                .toList();

        return ImpactAnalysisResponse.builder()
                .componentVersionId(version.getId())
                .versionNumber(version.getVersionNumber())
                .componentCode(version.getComponent().getComponentCode())
                .componentName(version.getComponent().getName())
                .totalAffectedAssemblies(usages.size())
                .affectedAssemblies(affectedList)
                .build();
    }

    /**
     * Find all assemblies using ANY version of a given component.
     *
     * <p>Useful for broader impact analysis:
     * "If we replace the entire Battery component family, what assemblies are affected?"
     */
    @Transactional(readOnly = true)
    public ImpactAnalysisResponse analyzeComponentImpact(Long componentId) {
        componentService.findComponentEntityById(componentId); // validate component exists

        List<AssemblyComponent> usages = assemblyComponentRepository.findByComponentId(componentId);

        log.info("Component-level impact analysis for component id {}: {} assembly usages found",
                componentId, usages.size());

        List<ImpactAnalysisResponse.AffectedAssemblyInfo> affectedList = usages.stream()
                .map(ac -> ImpactAnalysisResponse.AffectedAssemblyInfo.builder()
                        .assemblyId(ac.getAssembly().getId())
                        .assemblyName(ac.getAssembly().getName())
                        .projectId(ac.getAssembly().getProject().getId())
                        .projectName(ac.getAssembly().getProject().getName())
                        .versionAddedAt(ac.getAddedAt())
                        .build())
                .toList();

        // Use the component info from the first usage (or load from repo if no usages)
        String componentCode = usages.isEmpty()
                ? "ID:" + componentId
                : usages.get(0).getComponentVersion().getComponent().getComponentCode();
        String componentName = usages.isEmpty()
                ? "Unknown"
                : usages.get(0).getComponentVersion().getComponent().getName();

        return ImpactAnalysisResponse.builder()
                .componentVersionId(null) // Not version-specific
                .versionNumber("ALL VERSIONS")
                .componentCode(componentCode)
                .componentName(componentName)
                .totalAffectedAssemblies(affectedList.size())
                .affectedAssemblies(affectedList)
                .build();
    }
}
