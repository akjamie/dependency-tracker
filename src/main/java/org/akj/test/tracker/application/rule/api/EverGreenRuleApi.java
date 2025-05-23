package org.akj.test.tracker.application.rule.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.akj.test.tracker.application.rule.dto.EverGreenRuleDto;
import org.akj.test.tracker.application.rule.service.EverGreenRuleService;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
@Slf4j
public class EverGreenRuleApi {

    private final EverGreenRuleService everGreenRuleService;

    public EverGreenRuleApi(EverGreenRuleService everGreenRuleService) {
        this.everGreenRuleService = everGreenRuleService;
    }

    @PutMapping
    @Operation(
            summary = "Add a rule",
            description = "Add a rule to the system. This is used for rule management.",
            tags = "rules")
    EverGreenRuleDto addRule(@RequestBody @Valid EverGreenRuleDto rule) {
        log.info("Adding rule: {}", rule);
        // 1.except the basic validation, we should add more validations here

        // 2.call the service to add the rule
        EverGreenRuleDto greenRuleDto = everGreenRuleService.addRule(rule);
        log.info("Rule added, rule id:{}", greenRuleDto.getId());

        return greenRuleDto;
    }
}
