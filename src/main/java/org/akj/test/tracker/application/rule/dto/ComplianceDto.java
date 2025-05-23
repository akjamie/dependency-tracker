package org.akj.test.tracker.application.rule.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.rule.model.Severity;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ComplianceDto {
    @Future(message = "Deadline must be in the future")
    @NotNull
    private Instant deadline;

    @NotNull(message = "Severity is required")
    private Severity severity;
}