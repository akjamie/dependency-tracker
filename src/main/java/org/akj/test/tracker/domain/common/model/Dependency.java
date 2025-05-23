package org.akj.test.tracker.domain.common.model;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.*;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonPropertyOrder({"artefact", "version", "type"})
@EqualsAndHashCode
public class Dependency implements Serializable {
    // for maven, it's value is groupId:artifactId
    private String artefact;

    // actual version
    private String version;

    private String type;
}
