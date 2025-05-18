package org.akj.test.tracker.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import org.akj.test.tracker.domain.model.BuildManager;
import org.akj.test.tracker.domain.model.ProgramLanguage;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentDto {
    @NotNull
    private String name;
    @NotNull
    @Pattern(regexp = "^(https?:\\/\\/)?[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(:\\d+)?(\\/.*)?$")
    private String sourceCodeUrl;

    private ProgramLanguage language;

    private String eimId;

    @NotNull
    private BuildManager buildManager;

}
