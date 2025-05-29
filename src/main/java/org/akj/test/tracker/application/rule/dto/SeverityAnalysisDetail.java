package org.akj.test.tracker.application.rule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Severity analysis detail")
public class SeverityAnalysisDetail {
    @Schema(description = "Total number of rules with this severity")
    private long totalRules;

    @Schema(description = "Number of violations with this severity")
    private long violations;

    @Schema(description = "Number of components affected by violations of this severity")
    private long componentsAffected;
}