package org.akj.test.tracker.infrastructure.storage.component.repository;

import org.akj.test.tracker.domain.common.model.ProgramLanguage;
import org.akj.test.tracker.domain.component.model.BuildManager;
import org.akj.test.tracker.domain.component.model.ComponentAndDependency;
import org.akj.test.tracker.domain.component.service.ComponentDomainService;
import org.bson.types.ObjectId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComponentRepository extends MongoRepository<ComponentAndDependency, String> {
    ComponentAndDependency findByComponentIdAndBranch(String componentId, String branch);

    // find by branch + metadata.name + matadata.sourceCodeUrl
    @Query(
            value = "{'metadata.name': ?0, 'metadata.sourceCodeUrl': ?1, 'branch': ?2}",
            fields = "{'metadata.name': 1, 'metadata.sourceCodeUrl': 1, 'branch': 1}")
    ComponentAndDependency findByMetadataNameAndSourceCodeUrlAndBranch(String name, String sourceCodeUrl, String branch);

    @Query(value = """
                {
                    $and: [
                        { $or: [
                            { $expr: { $eq: [?0, null] } },
                            { $or: [
                                { 'metadata.name': { $regex: ?0, $options: 'i' } },
                                { 'metadata.sourceCodeUrl': { $regex: ?0, $options: 'i' } }
                            ]}
                        ]},
                        { $or: [
                            { $expr: { $eq: [?1, null] } },
                            { 'componentId': ?1 }
                        ]},
                        { $or: [
                            { $expr: { $eq: [?2, null] } },
                            { 'branch': ?2 }
                        ]},
                        { $or: [
                            { $expr: { $eq: [?3, null] } },
                            { 'runtimeVersion': ?3 }
                        ]},
                        { $or: [
                            { $expr: { $eq: [?4, null] } },
                            { 'compiler': ?4 }
                        ]},
                        { $or: [
                            { $expr: { $eq: [?5, null] } },
                            { 'language': ?5 }
                        ]},
                        { $or: [
                            { $expr: { $eq: [?6, null] } },
                            { 'buildManager': ?6 }
                        ]}
                    ]
                }
            """)
    Page<ComponentAndDependency> search(
            String q,
            String componentId,
            String branch,
            String runtimeVersion,
            String compiler,
            ProgramLanguage language,
            BuildManager buildManager,
            Pageable pageable
    );

    @Aggregation(pipeline = {
            "{ $project: { _id: 1, componentId: 1, branch: 1, metadata: 1 } }"
    })
    List<ComponentAndDependency> findAllComponents();

    @Query("{'_id': {'$oid': ?0}}")
    Optional<ComponentAndDependency> findByIdSafe(String id);
}
