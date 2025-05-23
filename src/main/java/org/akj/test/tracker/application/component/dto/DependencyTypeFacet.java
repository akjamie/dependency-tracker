package org.akj.test.tracker.application.component.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DependencyTypeFacet {
    private Map<String, Long> dependencyTypeDistribution;
    private Map<String, Long> dependenciesByLanguage;
}
