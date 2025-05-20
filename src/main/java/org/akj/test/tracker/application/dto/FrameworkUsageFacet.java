package org.akj.test.tracker.application.dto;

import lombok.Data;

import java.util.Map;

@Data
public class FrameworkUsageFacet {
    private Map<String, Long> springBootUsage;
    private Map<String, Long> reactUsage;
}