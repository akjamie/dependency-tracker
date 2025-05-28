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
            "{ $or: [ { 'status': ?2 }, { $expr: { $eq: [?2, null] } } ] } " +
            "] }")
    Page<RuleViolation> searchViolations(String ruleName, String componentName, ViolationStatus status, Pageable pageable);

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

}