package org.akj.test.tracker.application.rule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.rule.model.ViolationStatus;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request for searching rule violations")
public class RuleViolationSearchRequest {
    @Schema(description = "Rule name to filter violations (supports wildcard matching)")
    private String ruleName;

    @Schema(description = "Rule ID to filter violations")
    private String ruleId;

    @Schema(description = "Component name to filter violations (supports wildcard matching)")
    private String componentName;

    @Schema(description = "Status to filter violations")
    private ViolationStatus status;

    @Schema(description = "Page number (1-based)", required = true, defaultValue = "1")
    @Builder.Default
    @Min(1)
    private int page = 1;

    @Schema(description = "Number of items per page", required = true, defaultValue = "10")
    @Builder.Default
    @Min(1)
    private int size = 10;
}