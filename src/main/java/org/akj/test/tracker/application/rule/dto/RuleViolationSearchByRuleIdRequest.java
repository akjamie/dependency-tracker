package org.akj.test.tracker.application.rule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleViolationSearchByRuleIdRequest {
    @NotNull
    @NotBlank
    private String ruleId;

    private String componentName;

    @Builder.Default
    @Min(1)
    private int page = 1;

    @Builder.Default
    @Min(1)
    private int size = 20;
}
