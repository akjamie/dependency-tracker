package org.akj.test.tracker.domain.rule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.List;

@Document(collection = "rule_violations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleViolation {
    @MongoId
    private String id;
    // evergreen rule id
    private String ruleId;
    // id for componentAndDependency
    private String componentId;
    
    // Added fields for display
    private String ruleName;
    private String componentName;

    // runtime or artefact details
    private RuntimeTarget runtimeCurrentVersion;
    private RuntimeTarget runtimeTargetVersion;

    // current or target version
    List<DependencyViolation> dependencyViolations;

    // status
    private ViolationStatus status;

    private Boolean hasViolation;

    @Builder.Default
    private String createdBy = "System";
    @Builder.Default
    private String updatedBy = "System";
    private Instant createdAt;
    private Instant updatedAt;
    private Instant resolvedAt;
} 