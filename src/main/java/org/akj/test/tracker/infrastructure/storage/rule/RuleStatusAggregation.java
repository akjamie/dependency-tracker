package org.akj.test.tracker.infrastructure.storage.rule;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class RuleStatusAggregation {
    @Id
    private String id;
    private long count;
} 