package org.akj.test.tracker.infrastructure.storage.repository;

import org.akj.test.tracker.domain.model.ComponentAndDependency;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface ComponentRepository extends MongoRepository<ComponentAndDependency, String> {
    ComponentAndDependency findByComponentIdAndBranch(String componentId, String branch);

    // find by branch + metadata.name + matadata.sourceCodeUrl
    @Query(
            value = "{'metadata.name': ?0, 'metadata.sourceCodeUrl': ?1, 'branch': ?2}",
            fields = "{'metadata.name': 1, 'metadata.sourceCodeUrl': 1, 'branch': 1}")
    ComponentAndDependency findByMetadataNameAndSourceCodeUrlAndBranch(String name, String sourceCodeUrl, String branch);
}
