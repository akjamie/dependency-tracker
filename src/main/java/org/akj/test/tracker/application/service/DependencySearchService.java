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

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    public VersionDistributionFacet getVersionDistributionFacet() {
        try {
            // Validate collection exists and has data
            long totalComponents = mongoTemplate.getCollection(COLLECTION_NAME).countDocuments();
            log.info("Found {} documents in collection {}", totalComponents, COLLECTION_NAME);

            if (totalComponents == 0) {
                log.warn("Collection {} is empty", COLLECTION_NAME);
                return new VersionDistributionFacet();
            }

            // Aggregate runtime versions
            Aggregation runtimeVersionAgg = Aggregation.newAggregation(
                    Aggregation.group("runtimeVersion")
                            .count().as("count")
                            .addToSet("componentId").as("componentIds")
                            .push("buildManager").as("buildManagers")
            );

            // Aggregate Spring Boot versions
            Aggregation springBootAgg = Aggregation.newAggregation(
                    Aggregation.unwind("dependencies"),
                    Aggregation.match(Criteria.where("dependencies.artefact").regex("^org\\.springframework\\.boot")),
                    Aggregation.group("dependencies.version")
                            .count().as("count")
                            .addToSet("componentId").as("componentIds")
                            .push("dependencies.type").as("types")
                            .push("dependencies.artefact").as("artefacts")
            );

            // Aggregate frontend frameworks
            Aggregation frontendAgg = Aggregation.newAggregation(
                    Aggregation.unwind("dependencies"),
                    Aggregation.match(Criteria.where("dependencies.artefact").in(
                            "react", "react-dom", "angular", "vue"
                    )),
                    Aggregation.group("dependencies.artefact", "dependencies.version")
                            .count().as("count")
                            .addToSet("componentId").as("componentIds")
                            .push("dependencies.type").as("types")
            );

            // Execute aggregations
            List<Document> runtimeResults = mongoTemplate.aggregate(runtimeVersionAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> springBootResults = mongoTemplate.aggregate(springBootAgg, COLLECTION_NAME, Document.class).getMappedResults();
            List<Document> frontendResults = mongoTemplate.aggregate(frontendAgg, COLLECTION_NAME, Document.class).getMappedResults();

            // Build response
            VersionDistributionFacet facet = new VersionDistributionFacet();

            // Set metadata
            VersionDistributionFacet.Metadata metadata = new VersionDistributionFacet.Metadata();
            metadata.setLastUpdated(DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            metadata.setTotalComponents(totalComponents);
            facet.setMetadata(metadata);

            // Initialize version maps
            Map<String, VersionDistributionFacet.VersionInfo> javaVersions = new HashMap<>();
            Map<String, VersionDistributionFacet.VersionInfo> pythonVersions = new HashMap<>();
            Map<String, VersionDistributionFacet.VersionInfo> nodeVersions = new HashMap<>();
            Map<String, VersionDistributionFacet.VersionInfo> springBootVersions = new HashMap<>();
            Map<String, VersionDistributionFacet.VersionInfo> reactVersions = new HashMap<>();
            Map<String, VersionDistributionFacet.VersionInfo> angularVersions = new HashMap<>();
            Map<String, VersionDistributionFacet.VersionInfo> vueVersions = new HashMap<>();

            // Calculate total count for each runtime type
            long totalJavaCount = 0;
            long totalPythonCount = 0;
            long totalNodeCount = 0;

            for (Document doc : runtimeResults) {
                Object id = doc.get("_id");
                if (id == null) {
                    log.warn("Found document with null _id in runtime results");
                    continue;
                }

                String version = id.toString();
                long count = ((Number) doc.get("count")).longValue();

                if (version.toLowerCase().contains("jdk") || version.toLowerCase().contains("java")) {
                    totalJavaCount += count;
                } else if (version.toLowerCase().contains("python")) {
                    totalPythonCount += count;
                } else if (version.toLowerCase().contains("node")) {
                    totalNodeCount += count;
                }
            }

            // Process runtime versions with correct percentages
            for (Document doc : runtimeResults) {
                Object id = doc.get("_id");
                if (id == null) {
                    continue;
                }

                String version = id.toString();
                VersionDistributionFacet.VersionInfo info = new VersionDistributionFacet.VersionInfo();
                long count = ((Number) doc.get("count")).longValue();
                info.setCount(count);

                // Set percentage based on runtime type
                if (version.toLowerCase().contains("jdk") || version.toLowerCase().contains("java")) {
                    info.setPercentage(totalJavaCount > 0 ? (double) count / totalJavaCount * 100 : 0);
                } else if (version.toLowerCase().contains("python")) {
                    info.setPercentage(totalPythonCount > 0 ? (double) count / totalPythonCount * 100 : 0);
                } else if (version.toLowerCase().contains("node")) {
                    info.setPercentage(totalNodeCount > 0 ? (double) count / totalNodeCount * 100 : 0);
                }

                // Handle componentIds
                List<?> componentIds = (List<?>) doc.get("componentIds");
                if (componentIds != null) {
                    info.setComponentIds(componentIds.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .collect(Collectors.toList()));
                }

                // Handle build managers
                List<?> buildManagers = (List<?>) doc.get("buildManagers");
                if (buildManagers != null) {
                    Map<String, Long> buildManagerCount = buildManagers.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .collect(Collectors.groupingBy(
                                    Function.identity(),
                                    Collectors.counting()
                            ));
                    info.setDependencyTypes(buildManagerCount);
                }

                // Categorize by runtime type
                if (version.toLowerCase().contains("jdk") || version.toLowerCase().contains("java")) {
                    javaVersions.put(version, info);
                } else if (version.toLowerCase().contains("python")) {
                    pythonVersions.put(version, info);
                } else if (version.toLowerCase().contains("node")) {
                    nodeVersions.put(version, info);
                }
            }

            // Calculate total count for Spring Boot versions
            long totalSpringBootCount = springBootResults.stream()
                    .mapToLong(doc -> ((Number) doc.get("count")).longValue())
                    .sum();

            // Process Spring Boot versions with correct percentages
            for (Document doc : springBootResults) {
                Object id = doc.get("_id");
                if (id == null) {
                    continue;
                }

                String version = id.toString();
                VersionDistributionFacet.VersionInfo info = new VersionDistributionFacet.VersionInfo();
                long count = ((Number) doc.get("count")).longValue();
                info.setCount(count);
                info.setPercentage(totalSpringBootCount > 0 ? (double) count / totalSpringBootCount * 100 : 0);

                // Handle componentIds
                List<?> componentIds = (List<?>) doc.get("componentIds");
                if (componentIds != null) {
                    info.setComponentIds(componentIds.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .collect(Collectors.toList()));
                }

                // Handle dependency types and artefacts
                List<?> types = (List<?>) doc.get("types");
                List<?> artefacts = (List<?>) doc.get("artefacts");
                if (types != null && artefacts != null && types.size() == artefacts.size()) {
                    Map<String, Long> typeCount = new HashMap<>();
                    for (int i = 0; i < types.size(); i++) {
                        Object type = types.get(i);
                        Object artefact = artefacts.get(i);
                        if (type != null && artefact != null) {
                            String typeStr = type.toString();
                            String artefactStr = artefact.toString();
                            String key = artefactStr.contains("spring-boot-starter") ? "spring-boot-starter" :
                                    artefactStr.contains("spring-boot-actuator") ? "spring-boot-actuator" :
                                            artefactStr.contains("spring-boot-test") ? "spring-boot-test" : typeStr;
                            typeCount.merge(key, 1L, Long::sum);
                        }
                    }
                    info.setDependencyTypes(typeCount);
                }

                springBootVersions.put(version, info);
            }

            // Calculate total counts for frontend frameworks
            Map<String, Long> totalFrontendCounts = new HashMap<>();
            for (Document doc : frontendResults) {
                Object id = doc.get("_id");
                if (id == null) {
                    continue;
                }

                Document idDoc = (Document) id;
                String framework = idDoc.getString("dependencies.artefact");
                if (framework == null) {
                    continue;
                }

                long count = ((Number) doc.get("count")).longValue();
                totalFrontendCounts.merge(framework, count, Long::sum);
            }

            // Process frontend framework versions with correct percentages
            for (Document doc : frontendResults) {
                Object id = doc.get("_id");
                if (id == null) {
                    continue;
                }

                Document idDoc = (Document) id;
                String framework = idDoc.getString("dependencies.artefact");
                String version = idDoc.getString("dependencies.version");

                if (framework == null || version == null) {
                    continue;
                }

                VersionDistributionFacet.VersionInfo info = new VersionDistributionFacet.VersionInfo();
                long count = ((Number) doc.get("count")).longValue();
                info.setCount(count);

                // Set percentage based on framework type
                Long totalFrameworkCount = totalFrontendCounts.get(framework);
                info.setPercentage(totalFrameworkCount != null && totalFrameworkCount > 0 ?
                        (double) count / totalFrameworkCount * 100 : 0);

                // Handle componentIds
                List<?> componentIds = (List<?>) doc.get("componentIds");
                if (componentIds != null) {
                    info.setComponentIds(componentIds.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .collect(Collectors.toList()));
                }

                // Handle dependency types
                List<?> types = (List<?>) doc.get("types");
                if (types != null) {
                    Map<String, Long> typeCount = types.stream()
                            .filter(Objects::nonNull)
                            .map(Object::toString)
                            .collect(Collectors.groupingBy(
                                    Function.identity(),
                                    Collectors.counting()
                            ));
                    info.setDependencyTypes(typeCount);
                }

                // Categorize by framework
                switch (framework) {
                    case "react":
                    case "react-dom":
                        reactVersions.put(version, info);
                        break;
                    case "angular":
                        angularVersions.put(version, info);
                        break;
                    case "vue":
                        vueVersions.put(version, info);
                        break;
                }
            }

            // Set all version maps
            facet.setJavaVersions(javaVersions);
            facet.setPythonVersions(pythonVersions);
            facet.setNodeVersions(nodeVersions);
            facet.setSpringBootVersions(springBootVersions);
            facet.setReactVersions(reactVersions);
            facet.setAngularVersions(angularVersions);
            facet.setVueVersions(vueVersions);

            return facet;
        } catch (Exception e) {
            log.error("Error getting version distribution facet from collection {}: {}", COLLECTION_NAME, e.getMessage(), e);
            return new VersionDistributionFacet();
        }
    }
}
