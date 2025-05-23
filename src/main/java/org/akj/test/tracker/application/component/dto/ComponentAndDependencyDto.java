package org.akj.test.tracker.application.component.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.akj.test.tracker.domain.component.model.BuildManager;
import org.akj.test.tracker.domain.common.model.ProgramLanguage;

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
    @Valid
    private ComponentDto component;

    // usually, it's groupId:artifactId
    @NotNull
    private String componentId;
    @NotNull
    private String branch;

    @NotNull
    private String compiler;

    @NotNull
    @Valid
    private RuntimeInfoDto runtimeInfo;

    @NotNull
    private ProgramLanguage language;
    @NotNull
    private BuildManager buildManager;

    // some components have no dependencies
    @Valid
    @Builder.Default
    private List<DependencyDto> dependencies = new ArrayList<>();

    private Instant createdAt;
    private Instant lastUpdatedAt;
}
