package org.akj.test.tracker.application.component.dto;

import lombok.Data;

import java.util.Map;

@Data
public class TechnologyStackFacet {
    private Map<String, Long> languageDistribution;
    private Map<String, Long> buildManagerDistribution;
    private Map<String, Long> runtimeDistribution;
    private Map<String, Long> compilerDistribution;
}