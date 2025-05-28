package org.akj.test.tracker.application.rule.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.domain.component.service.ComponentDomainService;
import org.akj.test.tracker.infrastructure.utils.XxHashUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class EverGreenRuleViolationScheduler {
    private final EverGreenRuleViolationsScanService scanService;
    private final ComponentDomainService componentDomainService;

    @Scheduled(fixedDelay = 300000) // Default: daily at midnight
    public void scheduleRuleViolationScan() {
        String batchId = XxHashUtils.hash(String.valueOf(System.currentTimeMillis()));
        log.info("Starting scheduled rule violation scan, batchId: {}", batchId);
        try {
            // Get all components that need to be scanned
            List<ComponentDomainService.ComponentLite> components = componentDomainService.getAllComponents();
            if (components.isEmpty()) {
                log.info("No components found for rule violation scan, batchId: {}", batchId);
                return;
            }
            log.info("Found {} components to scan for rule violations, batchId: {}", components.size(), batchId);

            components.stream().forEach(component -> {
                try {
                    scanService.scanComponentForViolations(component);
                } catch (Exception e) {
                    log.error("Failed to scan component {} for violations", component.id(), e);
                }
            });
        } catch (Exception e) {
            log.error("Failed to execute scheduled rule violation scan, batchId: {}", batchId, e);
        }
        log.info("Completed scheduled rule violation scan, batchId: {}", batchId);
    }
} 