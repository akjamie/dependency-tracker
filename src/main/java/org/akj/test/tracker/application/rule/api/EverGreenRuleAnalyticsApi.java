package org.akj.test.tracker.application.rule.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.rule.dto.ComplianceSummaryResponse;
import org.akj.test.tracker.application.rule.dto.ComponentHealthResponse;
import org.akj.test.tracker.application.rule.dto.StatusAnalysisResponse;
import org.akj.test.tracker.application.rule.service.EverGreenRuleAnalyticsService;
import org.akj.test.tracker.infrastructure.config.spring.ApiResponse;
import org.akj.test.tracker.infrastructure.config.spring.BaseApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rule Analytics", description = "APIs for rule violation analytics")
public class EverGreenRuleAnalyticsApi extends BaseApi {
    private final EverGreenRuleAnalyticsService everGreenRuleAnalyticsService;

    @GetMapping("/compliance/summary")
    @Operation(summary = "Get compliance summary", description = "Get summary of active rules, components, and violations")
    public ResponseEntity<ApiResponse<ComplianceSummaryResponse>> getComplianceSummary() {
        log.debug("Getting compliance summary");
        try {
            ComplianceSummaryResponse response = everGreenRuleAnalyticsService.getComplianceSummary();
            return ok(response);
        } catch (Exception e) {
            log.error("Error getting compliance summary", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get compliance summary," + e.getMessage());
        }
    }

    @GetMapping("/rules/status")
    @Operation(summary = "Get rule status analysis", description = "Get distribution of violations by status")
    public ResponseEntity<ApiResponse<StatusAnalysisResponse>> getStatusAnalysis() {
        log.debug("Getting status analysis");
        try {
            StatusAnalysisResponse response = everGreenRuleAnalyticsService.getStatusAnalysis();
            return ok(response);
        } catch (Exception e) {
            log.error("Error getting status analysis", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Error getting status analysis," + e.getMessage());
        }
    }

    @GetMapping("/components/health")
    @Operation(summary = "Get component health", description = "Get top 10 components with most violations")
    public ResponseEntity<ApiResponse<ComponentHealthResponse>> getComponentHealth() {
        log.debug("Getting component health");
        try {
            ComponentHealthResponse response = everGreenRuleAnalyticsService.getComponentHealth();
            return ok(response);
        } catch (Exception e) {
            log.error("Error getting component health", e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get component health," + e.getMessage());
        }
    }
} 