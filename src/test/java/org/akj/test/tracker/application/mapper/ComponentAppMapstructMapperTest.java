package org.akj.test.tracker.application.mapper;

import org.akj.test.tracker.application.mapper.ComponentAppMapstructMapper;
import org.akj.test.tracker.application.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.dto.ComponentDto;
import org.akj.test.tracker.application.dto.DependencyDto;
import org.akj.test.tracker.domain.model.ComponentAndDependency;
import org.akj.test.tracker.domain.model.ComponentMetadata;
import org.akj.test.tracker.domain.model.Dependency;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

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
                new ComponentDto("componentName", "https://github.com/test", ProgramLanguage.JAVA, BuildManager.MAVEN, "eim456"),
                "componentId789",
                "main",
                Arrays.asList(new DependencyDto("org.example:artefact1", "1.0.0")),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // Act
        ComponentAndDependency domain = mapper.toDomain(dto);

        // Assert
        assertNotNull(domain);
        assertEquals(dto.getId(), domain.getId());
        assertEquals(dto.getComponent().getName(), domain.getMetadata().getName());
        assertEquals(dto.getComponent().getSourceCodeUrl(), domain.getMetadata().getSourceCodeUrl());
        assertEquals(dto.getComponent().getLanguage(), domain.getMetadata().getLanguage());
        assertEquals(dto.getComponent().getBuildManager(), domain.getMetadata().getBuildManager());
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
                new ComponentMetadata("componentName", "https://github.com/test", ProgramLanguage.JAVA, BuildManager.MAVEN, "eim456"),
                "componentId789",
                "main",
                "checksum123",
                Arrays.asList(new Dependency("org.example:artefact1", "1.0.0")),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // Act
        ComponentAndDependencyDto dto = mapper.toDto(domain);

        // Assert
        assertNotNull(dto);
        assertEquals(domain.getId(), dto.getId());
        assertNotNull(dto.getComponent());
        assertEquals(domain.getMetadata().getName(), dto.getComponent().getName());
        assertEquals(domain.getMetadata().getSourceCodeUrl(), dto.getComponent().getSourceCodeUrl());
        assertEquals(domain.getMetadata().getLanguage(), dto.getComponent().getLanguage());
        assertEquals(domain.getMetadata().getBuildManager(), dto.getComponent().getBuildManager());
        assertEquals(domain.getMetadata().getEimId(), dto.getComponent().getEimId());
        assertEquals(domain.getComponentId(), dto.getComponentId());
        assertEquals(domain.getBranch(), dto.getBranch());
        
        assertNotNull(dto.getDependencies());
        assertEquals(1, dto.getDependencies().size());
        assertEquals(domain.getDependencies().get(0).getArtefact(), dto.getDependencies().get(0).getArtefact());
        assertEquals(domain.getDependencies().get(0).getVersion(), dto.getDependencies().get(0).getVersion());
    }
}