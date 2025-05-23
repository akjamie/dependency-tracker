package org.akj.test.tracker.domain.rule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Target {
    /**
     * For runtime upgrades (e.g., JDK 8 -> JDK 11)
     */
    private RuntimeTarget runtimeTarget;

    /**
     * For dependency upgrades (e.g., Spring Boot 2.x -> 3.x)
     */
    private DependencyTarget dependencyTarget;
}