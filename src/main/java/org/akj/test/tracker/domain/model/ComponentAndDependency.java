package org.akj.test.tracker.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
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

    @MongoId
    private String id;

    private ComponentMetadata metadata;

    // usually, it's groupId:artifactId
    private String componentId;
    private String branch;

    private String checksum;
    @Builder.Default
    private List<Dependency> dependencies = new ArrayList<>();

    // for inactive component detection
    private Instant lastUpdatedAt;
    private Instant createdAt;
}
