package org.akj.test.tracker.application.rule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class TargetDto {
    /**
     * For runtime upgrades (e.g., JDK 8 -> JDK 11)
     */
    @Valid
    private RuntimeTargetDto runtimeTarget;

    /**
     * For dependency upgrades (e.g., Spring Boot 2.x -> 3.x)
     */
    @Valid
    private DependencyTargetDto dependencyTarget;

    @AssertTrue(message = "Either runtime target or dependency target must be specified")
    public boolean isAtLeastOneTargetPresent() {
        return runtimeTarget != null || dependencyTarget != null;
    }
}