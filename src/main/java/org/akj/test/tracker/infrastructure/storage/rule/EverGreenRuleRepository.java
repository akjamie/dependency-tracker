package org.akj.test.tracker.infrastructure.storage.rule;

import org.akj.test.tracker.domain.rule.model.EverGreenRule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface EverGreenRuleRepository extends MongoRepository<EverGreenRule, String> {
    
    @Query(value = "{ $and: [ " +
            "{ $or: [ { 'name': { $regex: ?0, $options: 'i' } }, { $expr: { $eq: [?0, null] } } ] }, " +
            "{ $or: [ { 'status': ?1 }, { $expr: { $eq: [?1, null] } } ] }, " +
            "{ $or: [ { 'language': ?2 }, { $expr: { $eq: [?2, null] } } ] }, " +
            "{ $or: [ " +
            "   { 'createdAt': { $gte: ?3, $lte: ?4 } }, " +
            "   { $expr: { $and: [ { $eq: [?3, null] }, { $eq: [?4, null] } ] } } " +
            "] } " +
            "] }")
    Page<EverGreenRule> searchRules(String name, String status, String language,
                                    Instant dateFrom, Instant dateTo, Pageable pageable);

    Optional<EverGreenRule> findByChecksum(String checksum);
}
