package org.akj.test.tracker.application.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.common.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.mapper.ComponentAppMapstructMapper;
import org.akj.test.tracker.domain.model.ComponentAndDependency;
import org.akj.test.tracker.domain.model.Dependency;
import org.akj.test.tracker.infrastructure.storage.repository.ComponentRepository;
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

        try {
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

        } catch (JsonProcessingException e) {
            log.error(
                    "Failed to calculate checksum for component: {}",
                    newComponent.getComponentId(),
                    e);
            throw new RuntimeException("Failed to process component dependencies", e);
        }
    }

    private void updateComponent(ComponentAndDependency existingComponent, String checksum,
                                 ComponentAndDependency newComponent) {
        existingComponent.setLastUpdatedAt(Instant.now());
        existingComponent.setDependencies(newComponent.getDependencies());
        existingComponent.setChecksum(checksum);
    }

    private String calculateChecksum(List<Dependency> dependencies) throws JsonProcessingException {
        if (dependencies == null || dependencies.isEmpty()) {
            return "";
        }
        String content = objectMapper.writeValueAsString(dependencies);

        return XxHashUtils.hash(content);
    }
}
