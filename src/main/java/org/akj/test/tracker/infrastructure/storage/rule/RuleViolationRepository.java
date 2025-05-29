package org.akj.test.tracker.infrastructure.storage.rule;

import org.akj.test.tracker.domain.rule.model.RuleViolation;
import org.akj.test.tracker.domain.rule.model.ViolationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import org.bson.Document;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Repository
public interface RuleViolationRepository extends MongoRepository<RuleViolation, String> {

    @Query(value = "{ $and: [ " +
            "{ $or: [ " +
            "   { $expr: { $eq: [?0, null] } }, " +
            "   { $expr: { $eq: [?0, ''] } }, " +
            "   { $expr: { $and: [ " +
            "       { $ne: [?0, null] }, " +
            "       { $ne: [?0, ''] }, " +
            "       { $regexMatch: { input: '$ruleName', regex: ?0, options: 'i' } } " +
            "   ] } } " +
            "] }, " +
            "{ $or: [ " +
            "   { $expr: { $eq: [?1, null] } }, " +
            "   { $expr: { $eq: [?1, ''] } }, " +
            "   { $expr: { $and: [ " +
            "       { $ne: [?1, null] }, " +
            "       { $ne: [?1, ''] }, " +
            "       { $regexMatch: { input: '$componentName', regex: ?1, options: 'i' } } " +
            "   ] } } " +
            "] }, " +
            "{ $or: [ { 'status': ?2 }, { $expr: { $eq: [?2, null] } } ] }, " +
            "{ $or: [ " +
            "   { $expr: { $eq: [?3, null] } }, " +
            "   { $expr: { $eq: [?3, ''] } }, " +
            "   { 'ruleId': ?3 } " +
            "] } " +
            "] }")
    Page<RuleViolation> searchViolations(String ruleName, String componentName, ViolationStatus status, String ruleId, Pageable pageable);

    @Query(value = "{ $and: [ " +
            "{ 'ruleId': ?0 }, " +
            "{ $or: [ " +
            "   { $expr: { $eq: [?1, null] } }, " +
            "   { $expr: { $eq: [?1, ''] } }, " +
            "   { $expr: { $and: [ " +
            "       { $ne: [?1, null] }, " +
            "       { $ne: [?1, ''] }, " +
            "       { $regexMatch: { input: '$componentName', regex: ?1, options: 'i' } } " +
            "   ] } } " +
            "] } " +
            "] }")
    Page<RuleViolation> searchViolationsByRuleId(String ruleId, String componentName, Pageable pageable);

    RuleViolation findByRuleIdAndComponentId(String ruleId, String componentId);

    @Aggregation(pipeline = {
        "{ $group: { " +
            "_id: '$componentId', " +
            "componentName: { $first: '$componentName' }, " +
            "totalViolations: { $sum: 1 } " +
        "} }",
        "{ $sort: { totalViolations: -1 } }",
        "{ $limit: 10 }"
    })
    List<ComponentViolationAggregation> getTopViolatedComponents();

    @Aggregation(pipeline = {
        "{ $group: { " +
            "_id: '$status', " +
            "count: { $sum: 1 } " +
        "} }"
    })
    List<StatusDistributionAggregation> getViolationStatusDistribution();

    @Query(value = "{ 'status': { $in: ['OPEN', 'IN_PROGRESS'] } }", count = true)
    long countActiveViolations();

    Page<RuleViolation> findByComponentId(String componentId, Pageable pageable);

    List<RuleViolation> findByComponentIdAndRuleId(String componentId, String ruleId);

    @Query(value = "{ 'componentId': ?0, 'status': { $in: ?1 } }")
    List<RuleViolation> findByComponentIdAndStatusIn(String componentId, List<ViolationStatus> statuses);

    @Query(value = "{ 'componentId': ?0, 'ruleId': ?1, 'status': { $in: ?2 } }")
    List<RuleViolation> findByComponentIdAndRuleIdAndStatusIn(String componentId, String ruleId, List<ViolationStatus> statuses);

    @Query(value = "{ 'status': { $in: ?0 } }")
    List<RuleViolation> findByStatusIn(List<ViolationStatus> statuses);

    @Query(value = "{ 'status': { $in: ?0 } }", count = true)
    long countByStatusIn(List<ViolationStatus> statuses);

    @Aggregation(pipeline = {
        "{ $facet: { " +
            "'runtime': [{ $match: { 'runtimeCurrentVersion': { $exists: true } } }, { $group: { _id: '$componentId', count: { $sum: 1 } } }], " +
            "'dependency': [{ $match: { 'dependencyViolations': { $exists: true, $ne: [] } } }, { $group: { _id: '$componentId', count: { $sum: { $size: { $ifNull: ['$dependencyViolations', []] } } } } }] " +
        "} }",
        "{ $project: { " +
            "totalViolations: { $add: [" +
                "{ $size: '$runtime' }, " +
                "{ $size: '$dependency' }" +
            "] } " +
        "} }"
    })
    ViolationCountAggregation getTotalViolationsCount();
}