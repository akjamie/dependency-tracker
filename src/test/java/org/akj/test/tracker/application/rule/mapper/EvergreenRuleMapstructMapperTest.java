package org.akj.test.tracker.application.rule.mapper;

import org.akj.test.tracker.application.rule.dto.*;
import org.akj.test.tracker.domain.common.model.ProgramLanguage;
import org.akj.test.tracker.domain.common.model.RuntimeType;
import org.akj.test.tracker.domain.rule.model.*;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class EvergreenRuleMapstructMapperTest {

    private final EverGreenRuleMapstructMapper mapper = EverGreenRuleMapstructMapper.INSTANCE;

    @Test
    public void testToDomain_fullMapping() {
        // Arrange
        EverGreenRuleDto dto = EverGreenRuleDto.builder()
                .id("123")
                .name("Test Rule")
                .description("This is a test rule.")
                .status(RuleStatus.ACTIVE)
                .ruleDefinition(
                        RuleDefinitionDto.builder()
                                .language(ProgramLanguage.JAVA)
                                .target(
                                        TargetDto.builder()
                                                .runtimeTarget(
                                                        RuntimeTargetDto.builder()
                                                                .runtimeType(RuntimeType.JDK)
                                                                .version("17.0.5")
                                                                .operator(VersionOperator.GREATER)
                                                                .build()
                                                )
                                                .dependencyTarget(
                                                        DependencyTargetDto.builder()
                                                                .artefact("org.example:library")
                                                                .version("2.1.0")
                                                                .operator(VersionOperator.EQUAL)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .compliance(
                        ComplianceDto.builder()
                                .deadline(Instant.now())
                                .severity(Severity.CRITICAL)
                                .build()
                )
                .build();

        // Act
        EverGreenRule domain = mapper.toDomain(dto);

        // Assert
        assertNotNull(domain);
        assertEquals(dto.getId(), domain.getId());
        assertEquals(dto.getName(), domain.getName());
        assertEquals(dto.getDescription(), domain.getDescription());
        assertEquals(dto.getStatus(), domain.getStatus());

        assertNotNull(domain.getRuleDefinition());
        assertEquals(dto.getRuleDefinition().getLanguage(), domain.getRuleDefinition().getLanguage());

        assertNotNull(domain.getRuleDefinition().getTarget());
        assertNotNull(domain.getRuleDefinition().getTarget().getRuntimeTarget());
        assertEquals(dto.getRuleDefinition().getTarget().getRuntimeTarget().getRuntimeType(),
                domain.getRuleDefinition().getTarget().getRuntimeTarget().getRuntimeType());
        assertEquals(dto.getRuleDefinition().getTarget().getRuntimeTarget().getVersion(),
                domain.getRuleDefinition().getTarget().getRuntimeTarget().getVersion());
        assertEquals(dto.getRuleDefinition().getTarget().getRuntimeTarget().getOperator(),
                domain.getRuleDefinition().getTarget().getRuntimeTarget().getOperator());

        assertNotNull(domain.getRuleDefinition().getTarget().getDependencyTarget());
        assertEquals(dto.getRuleDefinition().getTarget().getDependencyTarget().getArtefact(),
                domain.getRuleDefinition().getTarget().getDependencyTarget().getArtefact());
        assertEquals(dto.getRuleDefinition().getTarget().getDependencyTarget().getVersion(),
                domain.getRuleDefinition().getTarget().getDependencyTarget().getVersion());
        assertEquals(dto.getRuleDefinition().getTarget().getDependencyTarget().getOperator(),
                domain.getRuleDefinition().getTarget().getDependencyTarget().getOperator());

        assertNotNull(domain.getCompliance());
        assertEquals(dto.getCompliance().getDeadline(), domain.getCompliance().getDeadline());
        assertEquals(dto.getCompliance().getSeverity(), domain.getCompliance().getSeverity());
    }

    @Test
    public void testToDto_fullMapping() {
        // Arrange
        EverGreenRule rule = EverGreenRule.builder()
                .id("456")
                .name("Another Rule")
                .description("This is another test rule.")
                .status(RuleStatus.DRAFT)
                .ruleDefinition(
                        org.akj.test.tracker.domain.rule.model.RuleDefinition.builder()
                                .language(org.akj.test.tracker.domain.common.model.ProgramLanguage.PYTHON)
                                .target(
                                        org.akj.test.tracker.domain.rule.model.Target.builder()
                                                .runtimeTarget(
                                                        org.akj.test.tracker.domain.rule.model.RuntimeTarget.builder()
                                                                .runtimeType(RuntimeType.GROOVY)
                                                                .version("21.3.0")
                                                                .operator(org.akj.test.tracker.domain.rule.model.VersionOperator.LESS)
                                                                .build()
                                                )
                                                .dependencyTarget(
                                                        org.akj.test.tracker.domain.rule.model.DependencyTarget.builder()
                                                                .artefact("org.example:core")
                                                                .version("1.2.3-beta")
                                                                .operator(VersionOperator.CARET)
                                                                .build()
                                                )
                                                .build()
                                )
                                .build()
                )
                .compliance(
                        Compliance.builder()
                                .deadline(Instant.now().now().plus(1, ChronoUnit.DAYS))
                                .severity(Severity.LOW)
                                .build()
                )
                .build();

        // Act
        EverGreenRuleDto dto = mapper.toDto(rule);

        // Assert
        assertNotNull(dto);
        assertEquals(rule.getId(), dto.getId());
        assertEquals(rule.getName(), dto.getName());
        assertEquals(rule.getDescription(), dto.getDescription());
        assertEquals(rule.getStatus(), dto.getStatus());

        assertNotNull(dto.getRuleDefinition());
        assertEquals(rule.getRuleDefinition().getLanguage(), dto.getRuleDefinition().getLanguage());

        assertNotNull(dto.getRuleDefinition().getTarget());
        assertNotNull(dto.getRuleDefinition().getTarget().getRuntimeTarget());
        assertEquals(rule.getRuleDefinition().getTarget().getRuntimeTarget().getRuntimeType(),
                dto.getRuleDefinition().getTarget().getRuntimeTarget().getRuntimeType());
        assertEquals(rule.getRuleDefinition().getTarget().getRuntimeTarget().getVersion(),
                dto.getRuleDefinition().getTarget().getRuntimeTarget().getVersion());
        assertEquals(rule.getRuleDefinition().getTarget().getRuntimeTarget().getOperator(),
                dto.getRuleDefinition().getTarget().getRuntimeTarget().getOperator());

        assertNotNull(dto.getRuleDefinition().getTarget().getDependencyTarget());
        assertEquals(rule.getRuleDefinition().getTarget().getDependencyTarget().getArtefact(),
                dto.getRuleDefinition().getTarget().getDependencyTarget().getArtefact());
        assertEquals(rule.getRuleDefinition().getTarget().getDependencyTarget().getVersion(),
                dto.getRuleDefinition().getTarget().getDependencyTarget().getVersion());
        assertEquals(rule.getRuleDefinition().getTarget().getDependencyTarget().getOperator(),
                dto.getRuleDefinition().getTarget().getDependencyTarget().getOperator());

        assertNotNull(dto.getCompliance());
        assertEquals(rule.getCompliance().getDeadline(), dto.getCompliance().getDeadline());
        assertEquals(rule.getCompliance().getSeverity(), dto.getCompliance().getSeverity());
    }
}
