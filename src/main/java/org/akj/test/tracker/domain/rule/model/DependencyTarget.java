package org.akj.test.tracker.domain.rule.model;

import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyTarget {
    private String artefact;

    private String version;

    private VersionOperator operator;
}