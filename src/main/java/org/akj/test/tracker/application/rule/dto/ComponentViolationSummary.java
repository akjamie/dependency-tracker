package org.akj.test.tracker.application.rule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Component violation summary")
public class ComponentViolationSummary {
    @Schema(description = "Component ID")
    private String componentId;

    @Schema(description = "Component name")
    private String componentName;

    @Schema(description = "Total number of violations (runtime + dependency)")
    private int violationCount;
}