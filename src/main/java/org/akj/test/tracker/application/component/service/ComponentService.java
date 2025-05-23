package org.akj.test.tracker.application.component.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.component.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.component.mapper.ComponentAppMapstructMapper;
import org.akj.test.tracker.domain.component.model.ComponentAndDependency;
import org.akj.test.tracker.domain.common.model.Dependency;
import org.akj.test.tracker.infrastructure.storage.component.repository.ComponentRepository;
import org.akj.test.tracker.infrastructure.utils.XxHashUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class ComponentService {
    public static final String GENERATE_CHECKSUM_FAILED = "GENERATE_CHECKSUM_FAILED";
    private final ComponentRepository componentRepository;
    private final ComponentAppMapstructMapper componentAppMapstructMapper;
    private final ObjectMapper objectMapper;

    public ComponentService(
            ComponentRepository componentRepository,
            ComponentAppMapstructMapper componentAppMapstructMapper,
            @Qualifier("orderedObjectMapper") ObjectMapper objectMapper) {
        this.componentRepository = componentRepository;
        this.componentAppMapstructMapper = componentAppMapstructMapper;
        this.objectMapper = objectMapper;
    }

    public ComponentAndDependencyDto saveComponentAndDependency(
            ComponentAndDependencyDto componentAndDependencyDto) {
        log.info("Saving component and dependency: {}", componentAndDependencyDto);

        // 1. Convert to domain object
        ComponentAndDependency componentAndDependency =
                componentAppMapstructMapper.toDomain(componentAndDependencyDto);

        // 2. Find existing component by componentId + branch or name + sourceCodeUrl + branch
        ComponentAndDependency existingComponent = findExistingComponent(componentAndDependency);

        // 3. If exists, compare checksum and decide whether to update
        if (Objects.nonNull(existingComponent)) {
            return handleExistingComponent(componentAndDependency, existingComponent);
        }

        // 4. Save new component
        Instant now = Instant.now();
        componentAndDependency.setCreatedAt(now);
        componentAndDependency.setChecksum(
                calculateChecksum(componentAndDependency.getDependencies()));
        componentAndDependency.setLastUpdatedAt(now);
        componentRepository.save(componentAndDependency);
        return componentAppMapstructMapper.toDto(componentAndDependency);
    }

    private ComponentAndDependency findExistingComponent(ComponentAndDependency component) {
        if (StringUtils.isNotBlank(component.getComponentId())) {
            return componentRepository.findByComponentIdAndBranch(
                    component.getComponentId(), component.getBranch());
        } else {
            return componentRepository.findByMetadataNameAndSourceCodeUrlAndBranch(
                    component.getMetadata().getName(),
                    component.getMetadata().getSourceCodeUrl(),
                    component.getBranch());
        }
    }

    private ComponentAndDependencyDto handleExistingComponent(
            ComponentAndDependency newComponent, ComponentAndDependency existingComponent) {

        String newChecksum = calculateChecksum(newComponent.getDependencies());
        String existingChecksum = existingComponent.getChecksum();

        if (newChecksum.equals(existingChecksum)) {
            log.info(
                    "No changes detected, skipping update for component: {}@{}",
                    newComponent.getComponentId(),
                    newComponent.getBranch());
            return componentAppMapstructMapper.toDto(existingComponent);
        }

        // Update existing component with new data
        updateComponent(existingComponent, newChecksum, newComponent);
        componentRepository.save(existingComponent);
        return componentAppMapstructMapper.toDto(existingComponent);
    }

    private void updateComponent(ComponentAndDependency existingComponent, String checksum,
                                 ComponentAndDependency newComponent) {
        existingComponent.setLastUpdatedAt(Instant.now());
        existingComponent.setDependencies(newComponent.getDependencies());
        existingComponent.setChecksum(checksum);
    }

    private String calculateChecksum(List<Dependency> dependencies) {
        if (dependencies == null || dependencies.isEmpty()) {
            return "";
        }
        try {
            String content = null;
            content = objectMapper.writeValueAsString(dependencies);
            return XxHashUtils.hash(content);
        } catch (JsonProcessingException e) {
            log.warn("Failed to generate checksum for dependencies: {}", dependencies, e);
            return GENERATE_CHECKSUM_FAILED;
        }
    }
}
