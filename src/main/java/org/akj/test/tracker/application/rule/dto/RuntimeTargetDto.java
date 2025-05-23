package org.akj.test.tracker.application.rule.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.common.model.RuntimeType;
import org.akj.test.tracker.domain.rule.model.VersionOperator;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeTargetDto {
    @NotNull(message = "Runtime type is required")
    private RuntimeType runtimeType;
    
    @NotBlank(message = "Target version is required")
    @Size(min = 1, max = 30,
            message = "Version must be between 1 and 30 characters long")
    @Pattern(regexp = "^[0-9]+(\\.[0-9]+)*(-[a-zA-Z0-9]+)?$", message = "Invalid target version format")
    private String version;

    @NotNull
    private VersionOperator operator;
    
}