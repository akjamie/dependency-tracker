package org.akj.test.tracker.application.rule.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ComplianceSummaryResponse {
    private int activeRules;
    private int totalComponents;
    private int violatedComponents;
    private int activeViolations;
} 