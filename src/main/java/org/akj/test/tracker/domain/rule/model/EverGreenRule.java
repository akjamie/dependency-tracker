package org.akj.test.tracker.domain.rule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;

@Document(collection = "evergreen_rules")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EverGreenRule {
    @MongoId
    private String id;

    private String name;

    private String description;

    private RuleStatus status;

    private RuleDefinition ruleDefinition;

    private Compliance compliance;

    // todo: to be replaced with a proper user entity
    @Builder.Default
    private String createdBy = "System";
    @Builder.Default
    private String updatedBy = "System";
    private Instant createdAt;
    private Instant updatedAt;

    private String checksum;
}
