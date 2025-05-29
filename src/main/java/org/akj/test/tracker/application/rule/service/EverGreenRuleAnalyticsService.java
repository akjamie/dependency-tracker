package org.akj.test.tracker.application.rule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.rule.dto.*;
import org.akj.test.tracker.domain.rule.model.EverGreenRule;
import org.akj.test.tracker.domain.rule.model.RuleStatus;
import org.akj.test.tracker.domain.rule.model.RuleViolation;
import org.akj.test.tracker.infrastructure.storage.component.repository.ComponentRepository;
import org.akj.test.tracker.infrastructure.storage.rule.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class EverGreenRuleAnalyticsService {
    private final RuleViolationRepository ruleViolationRepository;
    private final EverGreenRuleRepository ruleRepository;
    private final ComponentRepository componentRepository;

    public ComplianceSummaryResponse getComplianceSummary() {
        log.debug("Getting compliance summary");
        
        // Get active rules count (ACTIVE + DRAFT)
        long activeRules = ruleRepository.countByStatusIn(List.of("ACTIVE", "DRAFT"));
        
        // Get total components count
        long totalComponents = componentRepository.count();
        
        // Get violated components count (unique components with violations)
        long violatedComponents = ruleViolationRepository.findAll().stream()
            .map(RuleViolation::getComponentId)
            .distinct()
            .count();
        
        // Get active violations count using the new aggregation
        ViolationCountAggregation violationCount = ruleViolationRepository.getTotalViolationsCount();
        long activeViolations = violationCount != null ? violationCount.getTotalViolations() : 0;

        return ComplianceSummaryResponse.builder()
            .activeRules((int) activeRules)
            .totalComponents((int) totalComponents)
            .violatedComponents((int) violatedComponents)
            .activeViolations((int) activeViolations)
            .build();
    }

    public StatusAnalysisResponse getStatusAnalysis() {
        log.debug("Getting rule status analysis");
        
        List<RuleStatusAggregation> statusDistribution = ruleRepository.getRuleStatusDistribution();
        
        Map<RuleStatus, Long> statusCounts = statusDistribution.stream()
            .collect(Collectors.toMap(
                s -> RuleStatus.valueOf(s.getId()),
                RuleStatusAggregation::getCount
            ));

        return StatusAnalysisResponse.builder()
            .statusDistribution(statusCounts)
            .build();
    }

    public ComponentHealthResponse getComponentHealth() {
        log.debug("Getting component health");
        
        List<ComponentViolationAggregation> topComponents = ruleViolationRepository.getTopViolatedComponents();
        
        List<ComponentViolationSummary> topViolatedComponents = topComponents.stream()
            .map(c -> ComponentViolationSummary.builder()
                .componentId(c.getId())
                .componentName(c.getComponentName())
                .violationCount(c.getTotalViolations())
                .build())
            .collect(Collectors.toList());

        return ComponentHealthResponse.builder()
            .topViolatedComponents(topViolatedComponents)
            .build();
    }
} 