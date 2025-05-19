package org.akj.test.tracker.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DependencyDto {
    // for maven, it's value is groupId:artifactId
    @NotNull
    private String artefact;

    // actual version
    @NotNull
    private String version;
}
