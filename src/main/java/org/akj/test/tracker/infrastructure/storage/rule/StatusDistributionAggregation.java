package org.akj.test.tracker.infrastructure.storage.rule;

import lombok.Data;
import org.springframework.data.annotation.Id;

@Data
public class StatusDistributionAggregation {
    @Id
    private String id;
    private long count;
} 