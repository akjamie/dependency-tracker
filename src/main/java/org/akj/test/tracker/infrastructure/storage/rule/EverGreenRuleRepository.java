package org.akj.test.tracker.infrastructure.storage.rule;

import org.akj.test.tracker.domain.rule.model.EverGreenRule;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EverGreenRuleRepository extends MongoRepository<EverGreenRule, String> {
}
