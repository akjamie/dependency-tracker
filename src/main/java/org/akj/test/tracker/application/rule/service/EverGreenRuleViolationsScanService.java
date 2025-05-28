package org.akj.test.tracker.application.rule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.domain.common.util.VersionComparator;
import org.akj.test.tracker.domain.component.model.ComponentAndDependency;
import org.akj.test.tracker.domain.component.model.RuntimeInfo;
import org.akj.test.tracker.domain.component.service.ComponentDomainService;
import org.akj.test.tracker.domain.rule.model.*;
import org.akj.test.tracker.infrastructure.storage.component.repository.ComponentRepository;
import org.akj.test.tracker.infrastructure.storage.rule.EverGreenRuleRepository;
import org.akj.test.tracker.infrastructure.storage.rule.RuleViolationRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class EverGreenRuleViolationsScanService {
    private final EverGreenRuleRepository everGreenRuleRepository;
    private final RuleViolationRepository ruleViolationRepository;
    private final MongoTemplate mongoTemplate;
    private final ComponentRepository componentRepository;
    private final ComponentDomainService componentDomainService;

    @Transactional
    public void scanComponentForViolations(ComponentDomainService.ComponentLite componentLite) {
        log.info("Starting violation scan for component, id:{}, componentId", componentLite.id(), componentLite.componentId());

        // Get component details
        ComponentAndDependency component = componentDomainService.getComponentById(componentLite.id());
        if (Objects.isNull(component)) {
            log.warn("Component not found for id: {}, skip violation scan.", componentLite.id());
            throw new RuntimeException("Component not found for id: " + componentLite.id());
        }

        // Get all rules for the component's language
        List<EverGreenRule> rules = everGreenRuleRepository.findByRuleDefinitionLanguageAndStatusIn(component.getLanguage(),
                List.of(RuleStatus.DRAFT, RuleStatus.ACTIVE));
        log.info("Found {} rules for component language: {}", rules.size(), component.getLanguage());

        List<RuleViolation> violations = new ArrayList<>();

        for (EverGreenRule rule : rules) {
            log.debug("Checking rule: id={}, name={}, status={}", rule.getId(), rule.getName(), rule.getStatus());

            // Find existing violation for this rule and component
            RuleViolation existingViolation = ruleViolationRepository
                    .findByRuleIdAndComponentId(rule.getId(), componentLite.id());

            RuleViolation violation = checkRuleViolations(rule, component, existingViolation);
            if (violation != null) {
                violations.add(violation);
                log.info("Found violation for rule: id={}, status={}", rule.getId(), violation.getStatus());
            }
        }

        // Save all violations
        if (!violations.isEmpty()) {
            ruleViolationRepository.saveAll(violations);
            log.info("Saved {} violations for component: {}", violations.size(), componentLite.id());
        } else {
            log.info("No violations found for component: {}", componentLite.id());
        }
    }

    private RuleViolation checkRuleViolations(EverGreenRule rule, ComponentAndDependency component, RuleViolation existingViolation) {
        Instant now = Instant.now();
        boolean hasViolation = false;

        // Create or get existing violation
        RuleViolation violation = existingViolation != null ? existingViolation :
                RuleViolation.builder()
                        .ruleId(rule.getId())
                        .componentId(component.getId())
                        .ruleName(rule.getName())
                        .componentName(component.getMetadata().getName())
                        .createdAt(now)
                        .createdBy("System")
                        .updatedBy("System")
                        .updatedAt(now)
                        .build();

        // Handle rule status changes
        if (rule.getStatus() == RuleStatus.DRAFT) {
            // For DRAFT rules, set status to IGNORED if not RESOLVED
            if (violation.getStatus() != ViolationStatus.RESOLVED) {
                log.info("Rule is in DRAFT status, setting violation status to IGNORED: ruleId={}, violationId={}",
                        rule.getId(), violation.getId());
                violation.setStatus(ViolationStatus.IGNORED);
                violation.setUpdatedAt(now);
                violation.setUpdatedBy("System");
            }
        } else if (rule.getStatus() == RuleStatus.ACTIVE) {
            // For ACTIVE rules, if violation was IGNORED (due to previous DRAFT status)
            // and not RESOLVED, set it to OPEN
            if (violation.getStatus() == ViolationStatus.IGNORED && violation.getStatus() != ViolationStatus.RESOLVED) {
                log.info("Rule changed from DRAFT to ACTIVE, setting violation status to OPEN: ruleId={}, violationId={}",
                        rule.getId(), violation.getId());
                violation.setStatus(ViolationStatus.OPEN);
                violation.setUpdatedAt(now);
                violation.setUpdatedBy("System");
            }
        }

        // Check runtime violations
        if (rule.getRuleDefinition().getTarget().getRuntimeTarget() != null) {
            RuntimeTarget target = rule.getRuleDefinition().getTarget().getRuntimeTarget();
            RuntimeInfo runtimeInfo = component.getRuntimeInfo();

            if (runtimeInfo == null || !VersionComparator.isVersionCompliant(
                    runtimeInfo.getVersion(), target.getVersion(), target.getOperator())) {
                log.info("Runtime violation found: ruleId={}, current={}, target={}, operator={}",
                        rule.getId(), runtimeInfo != null ? runtimeInfo.getVersion() : "null",
                        target.getVersion(), target.getOperator());
                RuntimeTarget runtimeTarget = RuntimeTarget.builder()
                        .operator(VersionOperator.EQUAL)
                        .runtimeType(runtimeInfo.getType())
                        .version(runtimeInfo.getVersion())
                        .build();
                violation.setRuntimeCurrentVersion(runtimeTarget);
                violation.setRuntimeTargetVersion(target);
                hasViolation = true;
            }
        }

        // Check dependency violations
        if (rule.getRuleDefinition().getTarget().getDependencyTarget() != null) {
            List<DependencyViolation> dependencyViolations = checkDependencyViolations(rule, component);
            if (!dependencyViolations.isEmpty()) {
                log.info("Dependency violations found: ruleId={}, count={}", rule.getId(), dependencyViolations.size());
                violation.setDependencyViolations(dependencyViolations);
                hasViolation = true;
            }
        }

        // Update violation status and metadata
        if (hasViolation) {
            // Only set to OPEN if rule is ACTIVE and violation is not RESOLVED
            if (rule.getStatus() == RuleStatus.ACTIVE && violation.getStatus() != ViolationStatus.RESOLVED) {
                violation.setStatus(ViolationStatus.OPEN);
                violation.setHasViolation(true);
                violation.setUpdatedAt(now);
            }
            return violation;
        } else if (existingViolation != null) {
            // If no violation found but there was a previous violation, mark as RESOLVED
            log.info("Violation resolved: ruleId={}, componentId={}", rule.getId(), component.getId());
            existingViolation.setStatus(ViolationStatus.RESOLVED);
            existingViolation.setResolvedAt(now);
            existingViolation.setUpdatedAt(now);
            return existingViolation;
        }

        return null;
    }

    private List<DependencyViolation> checkDependencyViolations(EverGreenRule rule, ComponentAndDependency component) {
        List<DependencyViolation> violations = new ArrayList<>();
        DependencyTarget target = rule.getRuleDefinition().getTarget().getDependencyTarget();

        component.getDependencies().forEach(dependency -> {
            // here should be equals or start with
            if (dependency.getArtefact().equals(target.getArtefact()) || dependency.getArtefact().startsWith(target.getArtefact())) {
                boolean isCompliant = VersionComparator.isVersionCompliant(
                        dependency.getVersion(), target.getVersion(), target.getOperator());

                if (!isCompliant) {
                    log.debug("Dependency violation found: artefact={}, current={}, target={}, operator={}",
                            dependency.getArtefact(), dependency.getVersion(), target.getVersion(), target.getOperator());
                    DependencyTarget currentVersion = DependencyTarget.builder()
                            .artefact(dependency.getArtefact())
                            .version(dependency.getVersion())
                            .operator(target.getOperator())
                            .build();

                    DependencyTarget targetVersion = DependencyTarget.builder()
                            .artefact(target.getArtefact())
                            .version(target.getVersion())
                            .operator(target.getOperator())
                            .build();

                    violations.add(DependencyViolation.builder()
                            .dependencyCurrentVersion(currentVersion)
                            .dependencyTargetVersion(targetVersion)
                            .build());
                }
            }
        });

        return violations;
    }

} 