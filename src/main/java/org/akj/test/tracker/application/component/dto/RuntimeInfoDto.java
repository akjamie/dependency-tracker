package org.akj.test.tracker.application.component.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.common.model.RuntimeType;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RuntimeInfoDto {
    @NotNull
    private RuntimeType type;
    private String version;
}
