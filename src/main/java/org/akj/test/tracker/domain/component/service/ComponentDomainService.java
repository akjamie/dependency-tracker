package org.akj.test.tracker.domain.component.service;

import org.akj.test.tracker.domain.component.model.ComponentAndDependency;
import org.akj.test.tracker.domain.component.model.ComponentMetadata;
import org.akj.test.tracker.infrastructure.storage.component.repository.ComponentRepository;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.project;

@Service
public class ComponentDomainService {

    private final MongoTemplate mongoTemplate;
    private final ComponentRepository componentRepository;

    public ComponentDomainService(MongoTemplate mongoTemplate, ComponentRepository componentRepository) {
        this.mongoTemplate = mongoTemplate;
        this.componentRepository = componentRepository;
    }

    public record ComponentLite(String id, String componentId, String branch, ComponentMetadata metadata) {
    }


    public List<ComponentLite> getAllComponents() {
        // Fetch all components from the repository
        // use mongo aggregation to fetch data for  @Aggregation(pipeline = {
        //            "{ $project: { _id: 1, componentId: 1, branch: 1, metadata: 1 } }"
        //    })
        // This will return a list of ComponentLite objects containing only the necessary fields
        // Note: The aggregation pipeline is not strictly necessary here, as we can directly map the fields
        // to ComponentLite, but it can be useful if we want to perform additional transformations or filtering.
        // If you want to use aggregation, you can uncomment the following line:
        return mongoTemplate.aggregate(newAggregation(ComponentAndDependency.class, project("id", "componentId", "branch", "metadata")), ComponentLite.class).getMappedResults();

    }

    public ComponentAndDependency getComponentById(String id) {
        return mongoTemplate.findById(new ObjectId(id), ComponentAndDependency.class);
    }
}
