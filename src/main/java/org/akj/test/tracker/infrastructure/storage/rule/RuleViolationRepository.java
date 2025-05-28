package org.akj.test.tracker.infrastructure.storage.rule;

import org.akj.test.tracker.domain.rule.model.RuleViolation;
import org.akj.test.tracker.domain.rule.model.ViolationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RuleViolationRepository extends MongoRepository<RuleViolation, String> {
    
    @Query(value = "{ $and: [ " +
            "{ $or: [ { 'ruleId': ?0 }, { $expr: { $eq: [?0, null] } } ] }, " +
            "{ $or: [ { 'projectId': ?1 }, { $expr: { $eq: [?1, null] } } ] }, " +
            "{ $or: [ { 'status': ?2 }, { $expr: { $eq: [?2, null] } } ] }, " +
            "{ $or: [ " +
            "   { 'createdAt': { $gte: ?3, $lte: ?4 } }, " +
            "   { $expr: { $and: [ { $eq: [?3, null] }, { $eq: [?4, null] } ] } } " +
            "] } " +
            "] }")
    Page<RuleViolation> searchViolations(String ruleId, String projectId, ViolationStatus status,
                                       Instant dateFrom, Instant dateTo, Pageable pageable);


    RuleViolation findByRuleIdAndComponentId(String ruleId, String componentId);

    List<RuleViolation> findByRuleIdAndComponentIdAndStatus(String ruleId, String componentId, ViolationStatus violationStatus);
}