package org.akj.test.tracker.application.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.dto.*;
import org.akj.test.tracker.application.service.DependencySearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dependencies")
@Slf4j
public class DependencySearchApi {
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
    public DependencySearchResponse search(@RequestBody @Valid DependencySearchRequest request) {
        log.info("Received search request: {}", request);
        return dependencySearchService.search(request);
    }

    @GetMapping("/facets/technology")
    public TechnologyStackFacet getTechnologyStackFacet() {
        return dependencySearchService.getTechnologyStackFacet();
    }

    @GetMapping("/facets/versions")
    public VersionDistributionFacet getVersionDistributionFacet() {
        return dependencySearchService.getVersionDistributionFacet();
    }

    @GetMapping("/facets/activity")
    public ComponentActivityFacet getComponentActivityFacet() {
        return dependencySearchService.getComponentActivityFacet();
    }
}