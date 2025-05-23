package org.akj.test.tracker.domain.rule.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.akj.test.tracker.domain.common.model.ProgramLanguage;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleDefinition {
    private ProgramLanguage language;

    private Target target;
}
