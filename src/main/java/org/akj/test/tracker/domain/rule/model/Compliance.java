package org.akj.test.tracker.domain.rule.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Compliance {
    private Instant deadline;

    private Severity severity;
}
