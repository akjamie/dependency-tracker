package org.akj.test.tracker.application.api;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.dto.ComponentDto;
import org.akj.test.tracker.application.service.ComponentService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/components")
@Slf4j
public class ComponentApi {

    private final ComponentService componentService;

    public ComponentApi(ComponentService componentService) {
        this.componentService = componentService;
    }

    // extension for git repo scan
    @PostMapping
    @Operation(
            summary = "Register a component",
            description =
                    "Register a component with its dependencies. This is used for git repo scan and dependency analysis.",
            tags = "component")
    public ComponentDto register(@RequestBody @Valid ComponentDto componentDto) {
        log.info("Registering component: {}", componentDto);
        ComponentAndDependencyDto componentAndDependency =
                ComponentAndDependencyDto.builder().component(componentDto).build();
        componentService.saveComponentAndDependency(componentAndDependency);
        return componentDto;
    }

    // extension for git repo scan
    @DeleteMapping
    @Operation(
            summary = "Unregister a component",
            description = "Unregister a component with its dependencies.",
            tags = "component")
    public boolean unregister(@RequestBody @Valid ComponentDto componentDto) {
        log.info("Unregistering component: {}", componentDto);
        // TODO: Add proper deletion logic in service layer
        throw new UnsupportedOperationException("Not implemented yet");
    }

//    @GetMapping
//    public

    @PutMapping
    @Operation(
            summary = "Upload a component & its dependencies",
            description =
                    "Upload a component with its dependencies. This is used for git repo scan and dependency analysis.",
            tags = "component")
    public ComponentAndDependencyDto upload(
            @RequestBody @Valid ComponentAndDependencyDto componentAndDependencies) {

        // since basic validations has been covered in JSR 303 validation, we just need to check the
        // packageInfo(componentId)
        if (StringUtils.isBlank(componentAndDependencies.getComponentId())) {
            log.error(
                    "packageInfo cannot be null, which will be used to get EIM information. component: {}",
                    componentAndDependencies.getComponent());
            throw new IllegalArgumentException(
                    "packageInfo cannot be null, which will be used to get EIM information.");
        }

        log.info(
                "Uploading component: {} with dependencies size: {}",
                componentAndDependencies.getComponent(),
                componentAndDependencies.getDependencies().size());

        componentService.saveComponentAndDependency(componentAndDependencies);
        return componentAndDependencies;
    }
}
