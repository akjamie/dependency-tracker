package org.akj.test.tracker.application.rule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;
import org.akj.test.tracker.domain.rule.model.ViolationStatus;

import java.util.List;
import java.util.Map;

@Data
@Builder
@Schema(description = "Component health response")
public class ComponentHealthResponse {
    @Schema(description = "Top 10 components with most violations")
    private List<ComponentViolationSummary> topViolatedComponents;
}

