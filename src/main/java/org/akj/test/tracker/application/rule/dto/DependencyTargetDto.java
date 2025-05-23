package org.akj.test.tracker.application.rule.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.rule.model.VersionOperator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class DependencyTargetDto {
    private String artefact;

    @Pattern(regexp = "^[0-9]+(\\.[0-9]+)*(-[a-zA-Z0-9]+)?$",
            message = "Invalid version format. Expected format: x.y.z[-suffix]")
    @Size(min = 1, max = 30,
            message = "Version must be between 1 and 30 characters long")
    private String version;

    @NotNull
    private VersionOperator operator;
}