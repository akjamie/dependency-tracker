package org.akj.test.tracker.domain.rule.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.common.model.RuntimeType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuntimeTarget {
    private RuntimeType runtimeType;
    
    private String version;

    private VersionOperator operator;
    
}