package org.akj.test.tracker.domain.component.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.common.model.Dependency;
import org.akj.test.tracker.domain.common.model.ProgramLanguage;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "component_dependency")
public class ComponentAndDependency {

//    @MongoId
    @Id
    @Field("_id")
    private String id;

    private ComponentMetadata metadata;

    // usually, it's groupId:artifactId
    private String componentId;
    private String branch;

    private String compiler;

    // for java, it's jdk version
    private RuntimeInfo runtimeInfo;

    private ProgramLanguage language;
    private BuildManager buildManager;

    private String checksum;
    @Builder.Default
    private List<Dependency> dependencies = new ArrayList<>();

    // for inactive component detection
    private Instant lastUpdatedAt;
    private Instant createdAt;
}
