package org.akj.test.tracker.application.component.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.component.dto.*;
import org.akj.test.tracker.application.component.service.DependencySearchService;
import org.akj.test.tracker.infrastructure.config.spring.ApiResponse;
import org.akj.test.tracker.infrastructure.config.spring.BaseApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dependencies")
@Slf4j
public class DependencySearchApi extends BaseApi {
    private final DependencySearchService dependencySearchService;

    @Autowired
    public DependencySearchApi(DependencySearchService dependencySearchService) {
        this.dependencySearchService = dependencySearchService;
    }

    @PostMapping
    @Operation(
            summary = "Search components",
            description = "Search components with various criteria",
            tags = "dependency"
    )
    public ResponseEntity<ApiResponse<DependencySearchResponse>> search(@RequestBody @Valid DependencySearchRequest request) {
        log.info("Received search request: {}", request);
        return ok(dependencySearchService.search(request));
    }

    @GetMapping("/facets/technology")
    @Operation(
            summary = "Get technology stack facet",
            description = "Get technology stack facet",
            tags = "facets"
    )
    public ResponseEntity<ApiResponse<TechnologyStackFacet>> getTechnologyStackFacet() {
        return ok(dependencySearchService.getTechnologyStackFacet());
    }

    @GetMapping("/facets/versions")
    @Operation(
            summary = "Get version distribution facet",
            description = "Get version distribution facet",
            tags = "facets"
    )
    public ResponseEntity<ApiResponse<VersionDistributionFacet>> getVersionDistributionFacet() {
        return ok(dependencySearchService.getVersionDistributionFacet());
    }

    @GetMapping("/facets/activity")
    @Operation(
            summary = "Get component activity facet",
            description = "Get component activity facet",
            tags = "facets"
    )
    public ResponseEntity<ApiResponse<ComponentActivityFacet>> getComponentActivityFacet() {
        return ok(dependencySearchService.getComponentActivityFacet());
    }
}