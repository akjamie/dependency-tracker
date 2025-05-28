package org.akj.test.tracker.application.rule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.rule.model.DependencyViolation;
import org.akj.test.tracker.domain.rule.model.RuntimeTarget;
import org.akj.test.tracker.domain.rule.model.ViolationStatus;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleViolationDto {
    private String id;
    private String ruleId;
    private String componentId;
    private RuntimeTarget runtimeCurrentVersion;
    private RuntimeTarget runtimeTargetVersion;
    private List<DependencyViolation> dependencyViolations;
    private ViolationStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
    
}