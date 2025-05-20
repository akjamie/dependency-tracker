package org.akj.test.tracker.application.dto;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonPropertyOrder({"artefact", "version", "type"})
public class DependencyDto {
    // for maven, it's value is groupId:artifactId
    @NotNull
    private String artefact;

    // actual version
    @NotNull
    private String version;

    private String type;
}
