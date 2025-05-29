package org.akj.test.tracker.application.rule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.rule.dto.RuleViolationSearchResponse;
import org.akj.test.tracker.domain.rule.model.RuleViolation;
import org.akj.test.tracker.domain.rule.model.ViolationStatus;
import org.akj.test.tracker.infrastructure.storage.rule.RuleViolationRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Slf4j
@RequiredArgsConstructor
public class EverGreenRuleViolationsService {
    private final RuleViolationRepository ruleViolationRepository;

    public RuleViolationSearchResponse searchViolations(String ruleName, String componentName,
            ViolationStatus status, String ruleId, int page, int size) {
        log.debug("Searching violations with ruleName: {}, componentName: {}, status: {}, ruleId: {}, page: {}, size: {}",
                ruleName, componentName, status, ruleId, page, size);

        // Create pageable with sorting by createdAt desc
        Pageable pageable = PageRequest.of(
            page - 1,
            size,
            Sort.by(Sort.Direction.DESC, "_id")
        );

        // Convert search terms to wildcard pattern if not null or empty
        String ruleNamePattern = StringUtils.hasText(ruleName) ? 
            ".*" + ruleName.trim() + ".*" : null;
        String componentNamePattern = StringUtils.hasText(componentName) ? 
            ".*" + componentName.trim() + ".*" : null;

        var pageResult = ruleViolationRepository.searchViolations(
            ruleNamePattern,
            componentNamePattern,
            status,
            ruleId,
            pageable
        );

        return RuleViolationSearchResponse.builder()
                .violations(pageResult.getContent())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .currentPage(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .build();
    }

    public RuleViolationSearchResponse searchViolationsByRuleId(String ruleId, String componentName,
            int page, int size) {
        log.debug("Searching violations for ruleId: {}, componentName: {}, page: {}, size: {}",
                ruleId, componentName, page, size);

        // Create pageable with sorting by createdAt desc
        Pageable pageable = PageRequest.of(
            page - 1,
            size,
            Sort.by(Sort.Direction.DESC, "_id")
        );

        // Convert search terms to wildcard pattern if not null or empty
        String componentNamePattern = StringUtils.hasText(componentName) ? 
            ".*" + componentName.trim() + ".*" : null;

        var pageResult = ruleViolationRepository.searchViolationsByRuleId(
            ruleId,
            componentNamePattern,
            pageable
        );

        return RuleViolationSearchResponse.builder()
                .violations(pageResult.getContent())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .currentPage(pageResult.getNumber() + 1)
                .pageSize(pageResult.getSize())
                .build();
    }
}
