package org.akj.test.tracker.application.rule.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.rule.dto.EverGreenRuleDto;
import org.akj.test.tracker.application.rule.dto.EverGreenRuleSearchRequest;
import org.akj.test.tracker.application.rule.dto.EverGreenRuleSearchResponse;
import org.akj.test.tracker.application.rule.service.EverGreenRuleService;
import org.akj.test.tracker.infrastructure.config.spring.BaseApi;
import org.akj.test.tracker.infrastructure.config.spring.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rules")
@Slf4j
@Validated
public class EverGreenRuleApi extends BaseApi {

    private final EverGreenRuleService everGreenRuleService;

    public EverGreenRuleApi(EverGreenRuleService everGreenRuleService) {
        this.everGreenRuleService = everGreenRuleService;
    }

    @PutMapping
    @Operation(
            summary = "Add a rule",
            description = "Add a rule to the system. This is used for rule management.",
            tags = "rules")
    ResponseEntity<ApiResponse<EverGreenRuleDto>> addRule(@RequestBody @Valid EverGreenRuleDto rule) {
        log.info("Adding rule: {}", rule);
        try {
            EverGreenRuleDto greenRuleDto = everGreenRuleService.addRule(rule);
            log.info("Rule added successfully, rule id: {}", greenRuleDto.getId());
            return created(greenRuleDto);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid rule data: {}", e.getMessage());
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error adding rule: {}", e.getMessage(), e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to add rule: " + e.getMessage());
        }
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update a rule",
            description = "Update an existing rule in the system.",
            tags = "rules")
    ResponseEntity<ApiResponse<EverGreenRuleDto>> updateRule(
            @Parameter(description = "ID of the rule to update", required = true)
            @PathVariable String id,
            @RequestBody @Valid EverGreenRuleDto rule) {
        log.info("Updating rule with id: {}", id);
        try {
            if (!id.equals(rule.getId())) {
                return error(HttpStatus.BAD_REQUEST, "Path ID does not match rule ID");
            }
            EverGreenRuleDto updatedRule = everGreenRuleService.updateRule(rule);
            log.info("Rule updated successfully, rule id: {}", updatedRule.getId());
            return ok(updatedRule, "Rule updated successfully");
        } catch (IllegalArgumentException e) {
            log.warn("Invalid rule data: {}", e.getMessage());
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error updating rule: {}", e.getMessage(), e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update rule: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Get a rule by ID",
            description = "Retrieve a rule by its ID.",
            tags = "rules")
    ResponseEntity<ApiResponse<EverGreenRuleDto>> getRuleById(
            @Parameter(description = "ID of the rule to retrieve", required = true)
            @PathVariable String id) {
        log.info("Retrieving rule with id: {}", id);
        try {
            EverGreenRuleDto rule = everGreenRuleService.getRuleById(id);
            if (rule == null) {
                return error(HttpStatus.NOT_FOUND, "Rule not found with id: " + id);
            }
            return ok(rule);
        } catch (Exception e) {
            log.error("Error retrieving rule: {}", e.getMessage(), e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve rule: " + e.getMessage());
        }
    }

    @PostMapping("/search")
    @Operation(
            summary = "Search rules",
            description = "Search for rules based on various criteria.",
            tags = "rules")
    ResponseEntity<ApiResponse<EverGreenRuleSearchResponse>> searchRules(
            @RequestBody @Valid EverGreenRuleSearchRequest request) {
        
        log.info("Searching rules with criteria: {}", request);
        
        try {
            // Validate date range if provided
            if (request.getFromDate() != null && request.getToDate() != null 
                && request.getFromDate().isAfter(request.getToDate())) {
                return error(HttpStatus.BAD_REQUEST, "fromDate must be before toDate");
            }


            EverGreenRuleSearchResponse response = everGreenRuleService.searchRules(
                request.getName(),
                request.getLanguage() != null ? request.getLanguage().name() : null,
                request.getStatus() != null ? request.getStatus().name() : null,
                request.getFromDate(),
                request.getToDate(),
                request.getPage(),
                request.getSize()
            );

            return ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid search parameters: {}", e.getMessage());
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            log.error("Error searching rules: {}", e.getMessage(), e);
            return error(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search rules: " + e.getMessage());
        }
    }
}
