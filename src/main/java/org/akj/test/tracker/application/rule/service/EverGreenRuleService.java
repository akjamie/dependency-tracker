package org.akj.test.tracker.application.rule.service;

import org.akj.test.tracker.application.rule.dto.EverGreenRuleDto;
import org.akj.test.tracker.application.rule.mapper.EverGreenRuleMapstructMapper;
import org.akj.test.tracker.domain.rule.model.EverGreenRule;
import org.akj.test.tracker.infrastructure.storage.rule.EverGreenRuleRepository;
import org.springframework.stereotype.Service;

@Service
public class EverGreenRuleService {
    private final EverGreenRuleRepository everGreenRuleRepository;

    private final EverGreenRuleMapstructMapper everGreenRuleMapstructMapper;

    public EverGreenRuleService(EverGreenRuleRepository everGreenRuleRepository, EverGreenRuleMapstructMapper everGreenRuleMapstructMapper) {
        this.everGreenRuleRepository = everGreenRuleRepository;
        this.everGreenRuleMapstructMapper = everGreenRuleMapstructMapper;
    }

    public EverGreenRuleDto addRule(EverGreenRuleDto ruleDto) {
        // 1. map the dto to domain
        EverGreenRule everGreenRule = everGreenRuleMapstructMapper.toDomain(ruleDto);

        // 2. save the rule to the database
        EverGreenRule savedEverGreenRule = everGreenRuleRepository.save(everGreenRule);

        // 3. map the domain to dto
        return everGreenRuleMapstructMapper.toDto(savedEverGreenRule);
    }
}
