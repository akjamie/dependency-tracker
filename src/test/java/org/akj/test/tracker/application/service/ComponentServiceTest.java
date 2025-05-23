package org.akj.test.tracker.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.akj.test.tracker.application.component.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.component.dto.ComponentDto;
import org.akj.test.tracker.application.component.dto.DependencyDto;
import org.akj.test.tracker.application.component.mapper.ComponentAppMapstructMapper;
import org.akj.test.tracker.application.component.service.ComponentService;
import org.akj.test.tracker.domain.component.model.ComponentAndDependency;
import org.akj.test.tracker.domain.component.model.ComponentMetadata;
import org.akj.test.tracker.domain.common.model.Dependency;
import org.akj.test.tracker.infrastructure.storage.component.repository.ComponentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ComponentServiceTest {

    @Mock
    private ComponentRepository componentRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    private ComponentService componentService;

    @Spy
    private ComponentAppMapstructMapper componentAppMapstructMapper = ComponentAppMapstructMapper.INSTANCE;

    @BeforeEach
    void setUp() {
        componentService = new ComponentService(
                componentRepository,
                componentAppMapstructMapper,
                objectMapper
        );
        // 初始化测试数据
        getComponentAndDependencyDto();

        ComponentAndDependency domainObject = new ComponentAndDependency();
        domainObject.setComponentId("test-component");
        domainObject.setBranch("main");
        domainObject.setMetadata(ComponentMetadata.builder()
                .sourceCodeUrl("https://example.com/test-component")
                .name("test-component")
                .build());
        domainObject.setDependencies(List.of(
                new Dependency("dep1", "1.0.0", "compile"),
                new Dependency("dep2", "2.0.0", "compile")
        ));
    }

    @Test
    void testSaveNewComponent() {
        // 模拟组件不存在
        when(componentRepository.findByComponentIdAndBranch(anyString(), anyString()))
                .thenReturn(null);

        ComponentAndDependencyDto testDto = getComponentAndDependencyDto();

        // 执行测试
        componentService.saveComponentAndDependency(testDto);

        // 验证调用次数
        verify(componentRepository, times(1)).save(any(ComponentAndDependency.class));
    }

    @Test
    void testSaveExistingComponentWithSameDependencies() {
        // 模拟现有组件
        ComponentAndDependency existing = new ComponentAndDependency();
        existing.setId("test-id");
        existing.setComponentId("test-package-info");
        existing.setBranch("main");
        existing.setChecksum("3bafbdf90833453b");
        existing.setMetadata(ComponentMetadata.builder()
                .sourceCodeUrl("https://example.com/test-component")
                .name("test-component")
                .build());
        existing.setDependencies(List.of(
                new Dependency("dep1", "1.0.0", "compile"),
                new Dependency("dep2", "2.0.0", "compile")
        ));

        ComponentAndDependencyDto testDto = getComponentAndDependencyDto();

        when(componentRepository.findByComponentIdAndBranch(anyString(), anyString()))
                .thenReturn(existing);

        // 执行测试
        componentService.saveComponentAndDependency(testDto);

        // 验证未调用保存
        verify(componentRepository, never()).save(any(ComponentAndDependency.class));
    }

    private static ComponentAndDependencyDto getComponentAndDependencyDto() {
        ComponentAndDependencyDto testDto = new ComponentAndDependencyDto();
        ComponentDto componentDto = new ComponentDto();
        testDto.setBranch("main");
        componentDto.setSourceCodeUrl("https://example.com/test-component");
        testDto.setComponentId("test-package-info");
        testDto.setComponent(componentDto);

        DependencyDto dep1 = new DependencyDto();
        dep1.setArtefact("dep1");
        dep1.setVersion("1.0.0");

        DependencyDto dep2 = new DependencyDto();
        dep2.setArtefact("dep2");
        dep2.setVersion("2.0.0");
        testDto.setDependencies(List.of(dep1, dep2));
        return testDto;
    }

    @Test
    void testSaveExistingComponentWithChangedDependencies() {
        // 模拟现有组件有不同依赖
        ComponentAndDependency existing = new ComponentAndDependency();
        existing.setId("test-id");
        existing.setComponentId("test-component");
        existing.setBranch("main");
        existing.setMetadata(ComponentMetadata.builder()
                .sourceCodeUrl("https://example.com/test-component")
                .name("test-component")
                .build());
        existing.setDependencies(List.of(
                new Dependency("dep1", "1.0.1", "compile"),
                new Dependency("dep2", "2.0.0", "compile")
        ));

        when(componentRepository.findByComponentIdAndBranch(anyString(), anyString()))
                .thenReturn(null);

        var testDto = getComponentAndDependencyDto();

        // 执行测试
        componentService.saveComponentAndDependency(testDto);

        // 验证调用保存
        verify(componentRepository, times(1)).save(any(ComponentAndDependency.class));
    }
}