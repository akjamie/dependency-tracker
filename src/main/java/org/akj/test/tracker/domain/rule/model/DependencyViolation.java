package org.akj.test.tracker.domain.rule.model;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Data
@Builder
@NoArgsConstructor
public class DependencyViolation {
    private DependencyTarget dependencyCurrentVersion;
    private DependencyTarget dependencyTargetVersion;

    public DependencyViolation(DependencyTarget dependencyCurrentVersion, DependencyTarget dependencyTargetVersion) {
        if (Objects.nonNull(dependencyCurrentVersion) && Objects.nonNull(dependencyTargetVersion)) {
            if (!dependencyCurrentVersion.getArtefact().equals(dependencyTargetVersion.getArtefact()) && !dependencyCurrentVersion.getArtefact().startsWith(dependencyTargetVersion.getArtefact())) {
                throw new IllegalArgumentException("Dependency artefacts do not match");
            }
        }

        this.dependencyCurrentVersion = dependencyCurrentVersion;
        this.dependencyTargetVersion = dependencyTargetVersion;
    }
}
