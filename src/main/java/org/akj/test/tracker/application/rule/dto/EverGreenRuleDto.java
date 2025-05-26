package org.akj.test.tracker.application.rule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.rule.model.RuleStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EverGreenRuleDto {
    private String id;

    @NotBlank(message = "Rule name is required")
    @Size(min = 3, max = 100, message = "Rule name must be between 3 and 100 characters")
    private String name;

    @NotBlank(message = "Rule description is required")
    @Size(max = 1000, message = "Description cannot exceed 500 characters")
    private String description;

    @NotNull(message = "Rule status is required")
    private RuleStatus status;

    @Valid
    @NotNull(message = "Rule definition is required")
    private RuleDefinitionDto ruleDefinition;

    @Valid
    @NotNull(message = "Compliance settings are required")
    private ComplianceDto compliance;

    // todo: to be replaced with a proper user entity
    @Builder.Default
    private String createdBy = "System";
    @Builder.Default
    private String updatedBy = "System";
    private String createdAt;
    private String updatedAt;

    private String checksum;
}