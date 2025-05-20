package org.akj.test.tracker.application.service;

import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.dto.DependencySearchRequest;
import org.akj.test.tracker.application.dto.DependencySearchResponse;
import org.akj.test.tracker.application.mapper.ComponentAppMapstructMapper;
import org.akj.test.tracker.domain.model.BuildManager;
import org.akj.test.tracker.domain.model.ComponentAndDependency;
import org.akj.test.tracker.domain.model.ProgramLanguage;
import org.akj.test.tracker.infrastructure.storage.repository.ComponentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DependencySearchService {
    private final ComponentRepository componentRepository;
    private final ComponentAppMapstructMapper componentAppMapstructMapper;
    private final MongoTemplate mongoTemplate;

    @Autowired
    public DependencySearchService(ComponentRepository componentRepository,
                                   ComponentAppMapstructMapper componentAppMapstructMapper, MongoTemplate mongoTemplate) {
        this.componentRepository = componentRepository;
        this.componentAppMapstructMapper = componentAppMapstructMapper;
        this.mongoTemplate = mongoTemplate;
    }

    public DependencySearchResponse search(DependencySearchRequest request) {
        log.info("Searching dependencies with request: {}", request);

        // Create pageable
        Pageable pageable = PageRequest.of(
            request.getPage() - 1,
            Math.min(request.getSize(), 100),
            Sort.by(
                request.getOrder() != null ? request.getOrder() : Sort.Direction.ASC,
                StringUtils.hasText(request.getSort()) ? request.getSort() : "metadata.name"
            )
        );

        // Execute search with null-safe parameters
        String searchQuery = StringUtils.hasText(request.getQ()) ? request.getQ() : null;
        String componentId = StringUtils.hasText(request.getComponentId()) ? request.getComponentId() : null;
        String branch = StringUtils.hasText(request.getBranch()) ? request.getBranch() : null;
        String runtimeVersion = StringUtils.hasText(request.getRuntimeVersion()) ? request.getRuntimeVersion() : null;
        String compiler = StringUtils.hasText(request.getCompiler()) ? request.getCompiler() : null;
        ProgramLanguage language = null;
        BuildManager buildManager = null;

        try {
            if (StringUtils.hasText(request.getLanguage())) {
                language = ProgramLanguage.valueOf(request.getLanguage());
            }
            if (StringUtils.hasText(request.getBuildManager())) {
                buildManager = BuildManager.valueOf(request.getBuildManager());
            }
        } catch (IllegalArgumentException e) {
            log.warn("Invalid enum value in search request: {}", e.getMessage());
        }

        // If searchQuery is null, we'll use a different query
        Page<ComponentAndDependency> page;
        if (searchQuery == null) {
            // Use a simpler query without regex when searchQuery is null
            Query query = new Query();
            
            if (componentId != null) {
                query.addCriteria(Criteria.where("componentId").is(componentId));
            }
            if (branch != null) {
                query.addCriteria(Criteria.where("branch").is(branch));
            }
            if (runtimeVersion != null) {
                query.addCriteria(Criteria.where("runtimeVersion").is(runtimeVersion));
            }
            if (compiler != null) {
                query.addCriteria(Criteria.where("compiler").is(compiler));
            }
            if (language != null) {
                query.addCriteria(Criteria.where("language").is(language));
            }
            if (buildManager != null) {
                query.addCriteria(Criteria.where("buildManager").is(buildManager));
            }
            
            query.with(pageable);
            List<ComponentAndDependency> content = mongoTemplate.find(query, ComponentAndDependency.class);
            long total = mongoTemplate.count(query, ComponentAndDependency.class);
            
            page = new PageImpl<>(content, pageable, total);
        } else {
            // Use the full search query with regex when searchQuery is not null
            page = componentRepository.search(
                searchQuery,
                componentId,
                branch,
                runtimeVersion,
                compiler,
                language,
                buildManager,
                pageable
            );
        }

        // Build response
        return DependencySearchResponse.builder()
            .metadata(buildMetadata(page))
            .data(page.getContent().stream()
                .map(componentAppMapstructMapper::toDto)
                .collect(Collectors.toList()))
            .build();
    }

    private DependencySearchResponse.Metadata buildMetadata(Page<ComponentAndDependency> page) {
        return DependencySearchResponse.Metadata.builder()
            .total(page.getTotalElements())
            .page(page.getNumber() + 1)
            .size(page.getSize())
            .totalPages(page.getTotalPages())
            .build();
    }
}