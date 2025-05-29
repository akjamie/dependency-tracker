package org.akj.test.tracker.application.rule.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.rule.dto.RuleViolationSearchByRuleIdRequest;
import org.akj.test.tracker.application.rule.dto.RuleViolationSearchRequest;
import org.akj.test.tracker.application.rule.dto.RuleViolationSearchResponse;
import org.akj.test.tracker.application.rule.service.EverGreenRuleViolationsService;
import org.akj.test.tracker.infrastructure.config.spring.ApiResponse;
import org.akj.test.tracker.infrastructure.config.spring.BaseApi;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rule-violations")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Rule Violations", description = "APIs for managing rule violations")
public class EverGreenRuleViolationApi extends BaseApi {
    private final EverGreenRuleViolationsService everGreenRuleViolationsService;

    @GetMapping
    @Operation(summary = "Search rule violations")
    public ResponseEntity<ApiResponse<RuleViolationSearchResponse>> searchViolations(
            @Valid RuleViolationSearchRequest request) {
        log.info("Searching rule violations with criteria: {}", request);

        try {
            RuleViolationSearchResponse response = everGreenRuleViolationsService.searchViolations(
                    request.getRuleName(),
                    request.getComponentName(),
                    request.getStatus(),
                    request.getRuleId(),
                    request.getPage(),
                    request.getSize()
            );

            return ok(response);
        } catch (Exception e) {
            log.error("Error searching rule violations: {}", e.getMessage(), e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search rule violations: " + e.getMessage());
        }
    }

    @GetMapping("/{ruleId}/violations")
    @Operation(summary = "Get violations for a specific rule")
    public ResponseEntity<ApiResponse<RuleViolationSearchResponse>> getViolationsByRuleId(
            @Parameter(description = "ID of the rule", required = true)
            @PathVariable String ruleId,
            @Valid RuleViolationSearchByRuleIdRequest request) {
        log.info("Getting violations for rule: {}, with criteria: {}", ruleId, request);

        try {
            if (!ruleId.equals(request.getRuleId())) {
                return error(HttpStatus.BAD_REQUEST, "Rule ID in path does not match request body");
            }

            RuleViolationSearchResponse response = everGreenRuleViolationsService.searchViolationsByRuleId(
                    ruleId,
                    request.getComponentName(),
                    request.getPage(),
                    request.getSize()
            );

            return ok(response);
        } catch (Exception e) {
            log.error("Error getting violations for rule {}: {}", ruleId, e.getMessage(), e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to get violations for rule: " + e.getMessage());
        }
    }
}