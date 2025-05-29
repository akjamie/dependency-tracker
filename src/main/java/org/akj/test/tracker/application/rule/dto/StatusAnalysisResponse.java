package org.akj.test.tracker.application.rule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.akj.test.tracker.domain.rule.model.RuleStatus;

import java.util.Map;

@Data
@Builder
@Schema(description = "Rule status analysis response")
public class StatusAnalysisResponse {
    @Schema(description = "Distribution of rules by status")
    private Map<RuleStatus, Long> statusDistribution;
} 