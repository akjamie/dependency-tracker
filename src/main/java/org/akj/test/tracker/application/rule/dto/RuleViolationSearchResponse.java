package org.akj.test.tracker.application.rule.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.rule.model.RuleViolation;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleViolationSearchResponse {
    private List<RuleViolation> violations;
    private long totalElements;
    private int totalPages;
    private int currentPage;
    private int pageSize;
} 