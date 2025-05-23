package org.akj.test.tracker.application.component.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

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

    private String eimId;

}
