package org.akj.test.tracker.application.mapper;

import org.akj.test.tracker.application.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.dto.ComponentDto;
import org.akj.test.tracker.application.dto.DependencyDto;
import org.akj.test.tracker.domain.model.BuildManager;
import org.akj.test.tracker.domain.model.ComponentAndDependency;
import org.akj.test.tracker.domain.model.ProgramLanguage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ComponentAppMapstructMapperTest {

    @InjectMocks
    private ComponentAppMapstructMapper mapper;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testToDomainMapping() {
        // Arrange
        ComponentAndDependencyDto dto = new ComponentAndDependencyDto(
                "id123",
                new ComponentDto("componentName", "https://github.com/test", "eim456"),
                "componentId789",
                "main",
                "/tools/jdk17",
                "jdk17",
                ProgramLanguage.JAVA,
                BuildManager.MAVEN,
                Arrays.asList(new DependencyDto("org.example:artefact1", "1.0.0")),
                Instant.now(),
                Instant.now()
        );

        // Act
        ComponentAndDependency domain = mapper.toDomain(dto);

        // Assert
        assertNotNull(domain);
        assertEquals(dto.getId(), domain.getId());
        assertEquals(dto.getComponent().getName(), domain.getMetadata().getName());
        assertEquals(dto.getComponent().getSourceCodeUrl(), domain.getMetadata().getSourceCodeUrl());
        assertEquals(dto.getLanguage(), domain.getLanguage());
        assertEquals(dto.getBuildManager(), domain.getBuildManager());
        assertEquals(dto.getComponent().getEimId(), domain.getMetadata().getEimId());
        assertEquals(dto.getComponentId(), domain.getComponentId());
        assertEquals(dto.getBranch(), domain.getBranch());

        assertNotNull(domain.getDependencies());
        assertEquals(1, domain.getDependencies().size());
        assertEquals(dto.getDependencies().get(0).getArtefact(), domain.getDependencies().get(0).getArtefact());
        assertEquals(dto.getDependencies().get(0).getVersion(), domain.getDependencies().get(0).getVersion());
    }

    @Test
    public void testToDtoMapping() {
        // Arrange
        ComponentAndDependency domain = new ComponentAndDependency(
                "id123",
                new ComponentDto("componentName", "https://github.com/test", "eim456"),
                "componentId789",
                "main",
                "/tools/jdk17",
                "jdk17",
                ProgramLanguage.JAVA,
                BuildManager.MAVEN,
                "xx9080jj&jkjl",
                Arrays.asList(new DependencyDto("org.example:artefact1", "1.0.0")),
                Instant.now(),
                Instant.now()
        );

        // Act
        ComponentAndDependencyDto dto = mapper.toDto(domain);

        // Assert
        assertNotNull(dto);
        assertEquals(domain.getId(), dto.getId());
        assertNotNull(dto.getComponent());
        assertEquals(domain.getMetadata().getName(), dto.getComponent().getName());
        assertEquals(domain.getMetadata().getSourceCodeUrl(), dto.getComponent().getSourceCodeUrl());
        assertEquals(domain.getLanguage(), dto.getLanguage());
        assertEquals(domain.getBuildManager(), dto.getBuildManager());
        assertEquals(domain.getMetadata().getEimId(), dto.getComponent().getEimId());
        assertEquals(domain.getComponentId(), dto.getComponentId());
        assertEquals(domain.getBranch(), dto.getBranch());

        assertNotNull(dto.getDependencies());
        assertEquals(1, dto.getDependencies().size());
        assertEquals(domain.getDependencies().get(0).getArtefact(), dto.getDependencies().get(0).getArtefact());
        assertEquals(domain.getDependencies().get(0).getVersion(), dto.getDependencies().get(0).getVersion());
    }
}