package org.akj.test.tracker.application.rule.dto;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}