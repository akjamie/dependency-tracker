package org.akj.test.tracker.application.rule.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.common.model.ProgramLanguage;
import org.akj.test.tracker.domain.rule.model.RuleStatus;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EverGreenRuleSearchRequest {
    private String name;
    private ProgramLanguage language;
    private RuleStatus status;
    private LocalDate fromDate;
    private LocalDate toDate;

    @Builder.Default
    @Min(value = 1, message = "Page number must be greater than 0")
    private int page = 1;

    @Builder.Default
    @Max(value = 100, message = "Page size must be less than or equal to 100")
    private int size = 20;
}
