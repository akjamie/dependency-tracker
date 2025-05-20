package org.akj.test.tracker.application.service;

import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.dto.*;
import org.akj.test.tracker.application.mapper.ComponentAppMapstructMapper;
import org.akj.test.tracker.domain.model.BuildManager;
import org.akj.test.tracker.domain.model.ComponentAndDependency;
import org.akj.test.tracker.domain.model.ProgramLanguage;
import org.akj.test.tracker.infrastructure.storage.repository.ComponentRepository;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
@Slf4j
public class DependencySearchService {
    public static final String COLLECTION_NAME = "component_dependency";
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

    private Map<String, Long> convertToMap(List<Document> results) {
        return results.stream()
                .collect(Collectors.toMap(
                        doc -> doc.get("_id").toString(),
                        doc -> ((Number) doc.get("count")).longValue()
                ));
    }

    public TechnologyStackFacet getTechnologyStackFacet() {
        try {
            // Validate collection exists and has data
            long count = mongoTemplate.getCollection(COLLECTION_NAME).countDocuments();
            log.info("Found {} documents in collection {}", count, COLLECTION_NAME);

            if (count == 0) {
                log.warn("Collection {} is empty", COLLECTION_NAME);
                return new TechnologyStackFacet();
            }

            // Create separate aggregations for each metric
            Aggregation languageAgg = Aggregation.newAggregation(
                    Aggregation.group("language")
                            .count().as("count")
            );

            Aggregation buildManagerAgg = Aggregation.newAggregation(
                    Aggregation.group("buildManager")
                            .count().as("count")
            );

            Aggregation runtimeAgg = Aggregation.newAggregation(
                    Aggregation.group("runtimeVersion")
                            .count().as("count")
            );

            Aggregation compilerAgg = Aggregation.newAggregation(
                    Aggregation.group("compiler")
                            .count().as("count")
            );

            // Execute aggregations
            List<Document> languageResults = mongoTemplate.aggregate(languageAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> buildManagerResults = mongoTemplate.aggregate(buildManagerAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> runtimeResults = mongoTemplate.aggregate(runtimeAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> compilerResults = mongoTemplate.aggregate(compilerAgg, COLLECTION_NAME, Document.class).getMappedResults();

            log.debug("Language results: {}", languageResults);
            log.debug("Build manager results: {}", buildManagerResults);
            log.debug("Runtime results: {}", runtimeResults);
            log.debug("Compiler results: {}", compilerResults);

            // Build facet response
            TechnologyStackFacet facet = new TechnologyStackFacet();
            facet.setLanguageDistribution(convertToMap(languageResults));
            facet.setBuildManagerDistribution(convertToMap(buildManagerResults));
            facet.setRuntimeDistribution(convertToMap(runtimeResults));
            facet.setCompilerDistribution(convertToMap(compilerResults));

            return facet;
        } catch (Exception e) {
            log.error("Error getting technology stack facet from collection {}: {}", COLLECTION_NAME, e.getMessage(), e);
            return new TechnologyStackFacet();
        }
    }

    public DependencyTypeFacet getDependencyTypeFacet() {
        try {
            // Validate collection exists and has data
            long count = mongoTemplate.getCollection(COLLECTION_NAME).countDocuments();
            if (count == 0) {
                log.warn("Collection {} is empty", COLLECTION_NAME);
                return new DependencyTypeFacet();
            }

            // Aggregate dependency types
            Aggregation typeAgg = Aggregation.newAggregation(
                    Aggregation.unwind("dependencies"),
                    Aggregation.group("dependencies.type")
                            .count().as("count")
            );

            // Aggregate dependencies by language
            Aggregation languageAgg = Aggregation.newAggregation(
                    Aggregation.unwind("dependencies"),
                    Aggregation.group("language", "dependencies.type")
                            .count().as("count")
            );

            // Execute aggregations
            List<Document> typeResults = mongoTemplate.aggregate(typeAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> languageResults = mongoTemplate.aggregate(languageAgg, COLLECTION_NAME, Document.class).getMappedResults();

            log.debug("Type results: {}", typeResults);
            log.debug("Language results: {}", languageResults);

            // Build facet response
            DependencyTypeFacet facet = new DependencyTypeFacet();
            facet.setDependencyTypeDistribution(convertToMap(typeResults));
            facet.setDependenciesByLanguage(convertToMap(languageResults));

            return facet;
        } catch (Exception e) {
            log.error("Error getting dependency type facet from collection {}: {}", COLLECTION_NAME, e.getMessage(), e);
            return new DependencyTypeFacet();
        }
    }

    public VersionPatternFacet getVersionPatternFacet() {
        try {
            // Validate collection exists and has data
            long count = mongoTemplate.getCollection(COLLECTION_NAME).countDocuments();
            if (count == 0) {
                log.warn("Collection {} is empty", COLLECTION_NAME);
                return new VersionPatternFacet();
            }

            // Aggregate version patterns
            Aggregation versionAgg = Aggregation.newAggregation(
                    Aggregation.unwind("dependencies"),
                    Aggregation.group("dependencies.version")
                            .count().as("count")
            );

            // Aggregate major versions
            Aggregation majorVersionAgg = Aggregation.newAggregation(
                    Aggregation.unwind("dependencies"),
                    Aggregation.project()
                            .andExpression("dependencies.version").as("majorVersion"),
                    Aggregation.match(Criteria.where("majorVersion").regex("^\\d+")),
                    Aggregation.group("majorVersion")
                            .count().as("count")
            );

            // Execute aggregations
            List<Document> versionResults = mongoTemplate.aggregate(versionAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> majorVersionResults = mongoTemplate.aggregate(majorVersionAgg, COLLECTION_NAME, Document.class).getMappedResults();

            log.debug("Version results: {}", versionResults);
            log.debug("Major version results: {}", majorVersionResults);

            // Build facet response
            VersionPatternFacet facet = new VersionPatternFacet();
            facet.setVersionPatternDistribution(convertToMap(versionResults));
            facet.setMajorVersionDistribution(convertToMap(majorVersionResults));

            return facet;
        } catch (Exception e) {
            log.error("Error getting version pattern facet from collection {}: {}", COLLECTION_NAME, e.getMessage(), e);
            return new VersionPatternFacet();
        }
    }

    public FrameworkUsageFacet getFrameworkUsageFacet() {
        try {
            // Validate collection exists and has data
            long count = mongoTemplate.getCollection(COLLECTION_NAME).countDocuments();
            log.info("Found {} documents in collection {}", count, COLLECTION_NAME);

            if (count == 0) {
                log.warn("Collection {} is empty", COLLECTION_NAME);
                return new FrameworkUsageFacet();
            }

            // Aggregate Spring Boot usage
            Aggregation springBootAgg = Aggregation.newAggregation(
                    Aggregation.unwind("dependencies"),
                    Aggregation.match(Criteria.where("dependencies.artefact").regex("^org\\.springframework\\.boot")),
                    Aggregation.group("dependencies.artefact")
                            .count().as("count")
            );

            // Aggregate React usage
            Aggregation reactAgg = Aggregation.newAggregation(
                    Aggregation.unwind("dependencies"),
                    Aggregation.match(Criteria.where("dependencies.artefact").regex("^react")),
                    Aggregation.group("dependencies.artefact")
                            .count().as("count")
            );

            // Execute aggregations
            List<Document> springBootResults = mongoTemplate.aggregate(springBootAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> reactResults = mongoTemplate.aggregate(reactAgg, COLLECTION_NAME, Document.class).getMappedResults();

            log.debug("Spring Boot results: {}", springBootResults);
            log.debug("React results: {}", reactResults);

            // Build facet response
            FrameworkUsageFacet facet = new FrameworkUsageFacet();
            facet.setSpringBootUsage(convertToMap(springBootResults));
            facet.setReactUsage(convertToMap(reactResults));

            return facet;
        } catch (Exception e) {
            log.error("Error getting framework usage facet from collection {}: {}", COLLECTION_NAME, e.getMessage(), e);
            return new FrameworkUsageFacet();
        }
    }

    public ComponentActivityFacet getComponentActivityFacet() {
        try {
            // Validate collection exists and has data
            long count = mongoTemplate.getCollection(COLLECTION_NAME).countDocuments();
            log.info("Found {} documents in collection {}", count, COLLECTION_NAME);

            if (count == 0) {
                log.warn("Collection {} is empty", COLLECTION_NAME);
                return new ComponentActivityFacet();
            }

            // Aggregate component types
            Aggregation typeAgg = Aggregation.newAggregation(
                    Aggregation.group("language", "buildManager")
                            .count().as("count")
            );

            // Aggregate dependency counts
            Aggregation depCountAgg = Aggregation.newAggregation(
                    Aggregation.project()
                            .and("language").as("language")
                            .and("dependencies").size().as("dependencyCount"),
                    Aggregation.group("language")
                            .avg("dependencyCount").as("avgCount")
            );

            // Execute aggregations
            List<Document> typeResults = mongoTemplate.aggregate(typeAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> depCountResults = mongoTemplate.aggregate(depCountAgg, COLLECTION_NAME, Document.class).getMappedResults();

            log.debug("Type results: {}", typeResults);
            log.debug("Dependency count results: {}", depCountResults);

            // Build facet response
            ComponentActivityFacet facet = new ComponentActivityFacet();
            
            // Convert type results
            Map<String, Long> componentTypes = new HashMap<>();
            for (Document doc : typeResults) {
                Document id = (Document) doc.get("_id");
                String key = id.get("language") + "_" + id.get("buildManager");
                Long value = ((Number) doc.get("count")).longValue();
                componentTypes.put(key, value);
            }
            
            // Convert dependency count results
            Map<String, Long> dependencyCount = new HashMap<>();
            for (Document doc : depCountResults) {
                String language = doc.get("_id").toString();
                Double avgCount = ((Number) doc.get("avgCount")).doubleValue();
                dependencyCount.put(language, avgCount.longValue());
            }

            facet.setComponentTypes(componentTypes);
            facet.setDependencyCount(dependencyCount);

            return facet;
        } catch (Exception e) {
            log.error("Error getting component activity facet from collection {}: {}", COLLECTION_NAME, e.getMessage(), e);
            return new ComponentActivityFacet();
        }
    }
}
