package org.akj.test.tracker.application.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class VersionDistributionFacet {
    private Metadata metadata;
    private Map<String, VersionInfo> javaVersions;
    private Map<String, VersionInfo> pythonVersions;
    private Map<String, VersionInfo> nodeVersions;
    private Map<String, VersionInfo> springBootVersions;
    private Map<String, VersionInfo> reactVersions;
    private Map<String, VersionInfo> angularVersions;
    private Map<String, VersionInfo> vueVersions;
    
    @Data
    public static class Metadata {
        private String lastUpdated;
        private Long totalComponents;
    }
    
    @Data
    public static class VersionInfo {
        private Long count;
        private Double percentage;
        private List<String> componentIds;
        private Map<String, Long> dependencyTypes;
    }
} 