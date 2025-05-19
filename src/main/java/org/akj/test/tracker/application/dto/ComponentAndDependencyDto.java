package org.akj.test.tracker.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.akj.test.tracker.domain.model.BuildManager;
import org.akj.test.tracker.domain.model.ProgramLanguage;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Setter
@EqualsAndHashCode
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComponentAndDependencyDto {
    private String id;

    @NotNull
    private ComponentDto component;

    // usually, it's groupId:artifactId
    @NotNull
    private String componentId;
    @NotNull
    private String branch;

    @NotNull
    private String compiler;

    // for java, it's jdk version
    private String runtimeVersion;

    @NotNull
    private ProgramLanguage language;
    @NotNull
    private BuildManager buildManager;

    // some components have no dependencies
    @Builder.Default
    private List<DependencyDto> dependencies = new ArrayList<>();

    private Instant createdAt;
    private Instant lastUpdatedAt;
}
