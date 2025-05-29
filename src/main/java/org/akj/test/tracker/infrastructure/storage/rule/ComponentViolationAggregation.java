package org.akj.test.tracker.infrastructure.storage.rule;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class ComponentViolationAggregation {
    @Id
    private String id;
    private String componentName;
    private int totalViolations;
} 