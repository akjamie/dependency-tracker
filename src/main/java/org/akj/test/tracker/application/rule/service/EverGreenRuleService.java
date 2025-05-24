package org.akj.test.tracker.application.rule.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.rule.dto.EverGreenRuleDto;
import org.akj.test.tracker.application.rule.dto.EverGreenRuleSearchResponse;
import org.akj.test.tracker.application.rule.mapper.EverGreenRuleMapstructMapper;
import org.akj.test.tracker.domain.rule.model.EverGreenRule;
import org.akj.test.tracker.infrastructure.storage.rule.EverGreenRuleRepository;
import org.akj.test.tracker.infrastructure.utils.XxHashUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class EverGreenRuleService {
    private static final String GENERATE_CHECKSUM_FAILED = "GENERATE_CHECKSUM_FAILED";

    private final EverGreenRuleRepository everGreenRuleRepository;
    private final EverGreenRuleMapstructMapper everGreenRuleMapstructMapper;
    private final ObjectMapper objectMapper;

    public EverGreenRuleService(
            EverGreenRuleRepository everGreenRuleRepository, EverGreenRuleMapstructMapper everGreenRuleMapstructMapper,
            @Qualifier("orderedObjectMapper") ObjectMapper objectMapper) {
        this.everGreenRuleRepository = everGreenRuleRepository;
        this.everGreenRuleMapstructMapper = everGreenRuleMapstructMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public EverGreenRuleDto addRule(EverGreenRuleDto ruleDto) {
        log.info("Adding new rule: {}", ruleDto);
        EverGreenRule rule = everGreenRuleMapstructMapper.toDomain(ruleDto);

        // Calculate checksum
        String checksum = calculateChecksum(rule);

        // Check if rule with same content exists
        Optional<EverGreenRule> existingRule = everGreenRuleRepository.findByChecksum(checksum);
        if (existingRule.isPresent()) {
            log.info("Rule with same content already exists with id: {}", existingRule.get().getId());
            return everGreenRuleMapstructMapper.toDto(existingRule.get());
        }

        // Set timestamps and checksum
        Instant now = Instant.now();
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);
        rule.setChecksum(checksum);

        EverGreenRule savedRule = everGreenRuleRepository.save(rule);
        log.info("Rule added successfully with id: {}", savedRule.getId());

        return everGreenRuleMapstructMapper.toDto(savedRule);
    }

    @Transactional
    public EverGreenRuleDto updateRule(EverGreenRuleDto ruleDto) {
        log.info("Updating rule: {}", ruleDto);
        validateRuleForUpdate(ruleDto);

        EverGreenRule existingRule = everGreenRuleRepository.findById(ruleDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("Rule not found with id: " + ruleDto.getId()));

        // Calculate new checksum
        EverGreenRule newRule = everGreenRuleMapstructMapper.toDomain(ruleDto);
        String newChecksum = calculateChecksum(newRule);

        // If content hasn't changed, return existing rule
        if (newChecksum.equals(existingRule.getChecksum())) {
            log.info("No changes detected in rule content, skipping update for rule: {}", ruleDto.getId());
            return everGreenRuleMapstructMapper.toDto(existingRule);
        }

        // Update existing rule
        everGreenRuleMapstructMapper.updateDomainFromDto(ruleDto, existingRule);
        existingRule.setUpdatedAt(Instant.now());
        existingRule.setChecksum(newChecksum);

        EverGreenRule updatedRule = everGreenRuleRepository.save(existingRule);
        log.info("Rule updated successfully with id: {}", updatedRule.getId());

        return everGreenRuleMapstructMapper.toDto(updatedRule);
    }

    private String calculateChecksum(EverGreenRule rule) {
        try {
            // Create a map of fields that should be included in checksum
            Map<String, Object> checksumContent = new HashMap<>();
//            checksumContent.put("name", rule.getName());
//            checksumContent.put("description", rule.getDescription());
            checksumContent.put("ruleDefinition", rule.getRuleDefinition());
//            checksumContent.put("compliance", rule.getCompliance());

            // Convert to JSON and calculate hash
            String content = objectMapper.writeValueAsString(checksumContent);
            return XxHashUtils.hash(content);
        } catch (JsonProcessingException e) {
            log.warn("Failed to generate checksum for rule: {}", rule, e);
            return GENERATE_CHECKSUM_FAILED;
        }
    }

    private void validateRuleForUpdate(EverGreenRuleDto ruleDto) {
        if (ruleDto.getId() == null || ruleDto.getId().trim().isEmpty()) {
            throw new IllegalArgumentException("Rule ID is required for update");
        }
    }

    public EverGreenRuleDto getRuleById(String id) {
        log.info("Retrieving rule with id: {}", id);
        return everGreenRuleRepository.findById(id)
                .map(everGreenRuleMapstructMapper::toDto)
                .orElse(null);
    }

    public EverGreenRuleSearchResponse searchRules(String name, String language, String status,
                                                 LocalDate fromDate, LocalDate toDate,
                                                 int page, int size) {
        log.info("Searching rules with criteria - name: {}, language: {}, status: {}, fromDate: {}, toDate: {}, page: {}, size: {}",
                name, language, status, fromDate, toDate, page, size);

        Pageable pageable = PageRequest.of(page - 1, size);
        Page<EverGreenRule> rulesPage = everGreenRuleRepository.searchRules(
                name != null ? name : "",
                status,
                language,
                fromDate,
                toDate,
                pageable
        );

        List<EverGreenRuleDto> ruleDtos = rulesPage.getContent().stream()
                .map(everGreenRuleMapstructMapper::toDto)
                .collect(Collectors.toList());

        return EverGreenRuleSearchResponse.builder()
                .metadata(EverGreenRuleSearchResponse.Metadata.builder()
                        .total(rulesPage.getTotalElements())
                        .page(page)
                        .size(size)
                        .totalPages(rulesPage.getTotalPages())
                        .build())
                .data(ruleDtos)
                .build();
    }
}