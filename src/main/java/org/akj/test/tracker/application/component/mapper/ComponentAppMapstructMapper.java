package org.akj.test.tracker.application.component.mapper;

import org.akj.test.tracker.application.component.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.component.dto.DependencyDto;
import org.akj.test.tracker.application.component.dto.RuntimeInfoDto;
import org.akj.test.tracker.domain.common.model.Dependency;
import org.akj.test.tracker.domain.component.model.ComponentAndDependency;
import org.akj.test.tracker.domain.component.model.RuntimeInfo;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = org.mapstruct.NullValueCheckStrategy.ALWAYS,
        unmappedTargetPolicy = ReportingPolicy.WARN)
public interface ComponentAppMapstructMapper {

    ComponentAppMapstructMapper INSTANCE = Mappers.getMapper(ComponentAppMapstructMapper.class);

    @Mapping(source = "component.name", target = "metadata.name")
    @Mapping(source = "component.sourceCodeUrl", target = "metadata.sourceCodeUrl")
    @Mapping(source = "component.eimId", target = "metadata.eimId")
    ComponentAndDependency toDomain(ComponentAndDependencyDto componentAndDependencyDto);


    @Mapping(source = "type", target = "type")
    Dependency toDomain(DependencyDto dependencyDto);

    List<Dependency> toDomainList(List<DependencyDto> dependencyDtos);

    List<DependencyDto> toDtoList(List<Dependency> dependencies);

    @Mapping(source = "metadata.name", target = "component.name")
    @Mapping(source = "metadata.sourceCodeUrl", target = "component.sourceCodeUrl")
    @Mapping(source = "metadata.eimId", target = "component.eimId")
    @Mapping(source = "runtimeInfo", target = "runtimeInfo")
    ComponentAndDependencyDto toDto(ComponentAndDependency componentAndDependency);

    @Mapping(source = "type", target = "type")
    @Mapping(source = "version", target = "version")
    RuntimeInfo toDomain(RuntimeInfoDto runtimeInfoDto);

    @Mapping(source = "type", target = "type")
    @Mapping(source = "version", target = "version")
    RuntimeInfoDto toDto(RuntimeInfo runtimeInfo);
}
