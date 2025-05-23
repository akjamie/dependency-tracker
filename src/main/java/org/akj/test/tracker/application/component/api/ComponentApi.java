package org.akj.test.tracker.application.component.api;

import io.micrometer.common.util.StringUtils;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.component.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.component.dto.ComponentDto;
import org.akj.test.tracker.application.component.service.ComponentService;
import org.akj.test.tracker.infrastructure.config.spring.ApiResponse;
import org.akj.test.tracker.infrastructure.config.spring.BaseApi;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/components")
@Slf4j
public class ComponentApi extends BaseApi {

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
    public ResponseEntity<ApiResponse<ComponentAndDependencyDto>> register(@RequestBody @Valid ComponentDto componentDto) {
        log.info("Registering component: {}", componentDto);
        ComponentAndDependencyDto componentAndDependency =
                ComponentAndDependencyDto.builder().component(componentDto).build();
        ComponentAndDependencyDto dependencyDto = componentService.saveComponentAndDependency(componentAndDependency);

        return ok(dependencyDto);
    }

    // extension for git repo scan
    @DeleteMapping
    @Operation(
            summary = "Unregister a component",
            description = "Unregister a component with its dependencies.",
            tags = "component")
    public ResponseEntity<ApiResponse<Boolean>> unregister(@RequestBody @Valid ComponentDto componentDto) {
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
    public ResponseEntity<ApiResponse<ComponentAndDependencyDto>> upload(
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
        return ok(componentAndDependencies);
    }
}
