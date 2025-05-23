package org.akj.test.tracker.application.component.dto;

import lombok.Data;

import java.util.Map;

@Data
public class ComponentActivityFacet {
    private Map<String, Long> componentTypes;
    private Map<String, Long> dependencyCount;
}
