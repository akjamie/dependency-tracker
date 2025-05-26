package org.akj.test.tracker.application.rule.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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
@JsonIgnoreProperties(ignoreUnknown = true)
public class RuleDefinitionDto {
    @NotNull
    private ProgramLanguage language;

    @Valid
    @NotNull(message = "Target is required")
    private TargetDto target;

}