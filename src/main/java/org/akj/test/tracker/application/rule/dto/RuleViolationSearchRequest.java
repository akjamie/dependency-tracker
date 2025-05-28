 package org.akj.test.tracker.application.rule.dto;

import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.rule.model.ViolationStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleViolationSearchRequest {
    private String ruleName;
    private String componentName;
    private ViolationStatus status;
    
    @Builder.Default
    @Min(1)
    private int page = 1;
    
    @Builder.Default
    @Min(1)
    private int size = 20;
}