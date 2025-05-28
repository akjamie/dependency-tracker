package org.akj.test.tracker.application.rule.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.akj.test.tracker.application.rule.dto.RuleViolationDto;
import org.akj.test.tracker.application.rule.service.EverGreenRuleViolationsScanService;
import org.akj.test.tracker.domain.rule.model.ViolationStatus;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/rule-violations")
@RequiredArgsConstructor
@Tag(name = "Rule Violations", description = "APIs for managing rule violations")
public class EverGreenRuleViolationApi {
   private final EverGreenRuleViolationsScanService scanService;

   @PostMapping("/scan/{componentId}")
   @Operation(summary = "Trigger violation scan for a component")
   public ResponseEntity<Void> triggerScan(@PathVariable String componentId) {
       scanService.scanComponentForViolations(componentId);
       return ResponseEntity.ok().build();
   }

   @GetMapping
   @Operation(summary = "Search rule violations")
   public ResponseEntity<Page<RuleViolationDto>> searchViolations(
           @RequestParam(required = false) String ruleId,
//            @RequestParam(required = false) String componentId,
           @RequestParam(required = false) ViolationStatus status,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateFrom,
           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant dateTo,
           @RequestParam(defaultValue = "1") int page,
           @RequestParam(defaultValue = "20") int size) {
       return ResponseEntity.ok(scanService.searchViolationsWithDetails(
               ruleId, componentId, status, dateFrom, dateTo, page, size));
   }

   @PatchMapping("/{violationId}/status")
   @Operation(summary = "Update violation status")
   public ResponseEntity<RuleViolationDto> updateViolationStatus(
           @PathVariable String violationId,
           @RequestParam ViolationStatus status,
           @RequestHeader("X-User-Id") String userId) {
       return ResponseEntity.ok(scanService.updateViolationStatus(violationId, status, userId));
   }

   @PostMapping("/{ruleId}/components/{componentId}/resolve")
   @Operation(summary = "Resolve all violations for a rule in a component")
   public ResponseEntity<Void> resolveAllViolationsForRule(
           @PathVariable String ruleId,
           @PathVariable String componentId,
           @RequestHeader("X-User-Id") String userId) {
       scanService.resolveAllViolationsForRule(ruleId, componentId, userId);
       return ResponseEntity.ok().build();
   }
}