package org.akj.test.tracker.application.component.dto;

import lombok.Data;

import java.util.Map;

@Data
public class VersionPatternFacet {
    private Map<String, Long> versionPatternDistribution;
    private Map<String, Long> majorVersionDistribution;
}