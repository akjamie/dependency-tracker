package org.akj.test.tracker.application.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.dto.DependencySearchRequest;
import org.akj.test.tracker.application.dto.DependencySearchResponse;
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
}