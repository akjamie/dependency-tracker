package org.akj.test.tracker.application.mapper;

import org.akj.test.tracker.application.dto.ComponentAndDependencyDto;
import org.akj.test.tracker.application.dto.ComponentDto;
import org.akj.test.tracker.application.dto.DependencyDto;
import org.akj.test.tracker.domain.model.ComponentAndDependency;
import org.akj.test.tracker.domain.model.Dependency;
import org.mapstruct.*;
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


    @Mapping(source="type", target = "type")
    Dependency toDomain(DependencyDto dependencyDto);

    List<Dependency> toDomainList(List<DependencyDto> dependencyDtos);

    List<DependencyDto> toDtoList(List<Dependency> dependencies);

    @Mapping(source = "metadata.name", target = "component.name")
    @Mapping(source = "metadata.sourceCodeUrl", target = "component.sourceCodeUrl")
    @Mapping(source = "metadata.eimId", target = "component.eimId")
    ComponentAndDependencyDto toDto(ComponentAndDependency componentAndDependency);

}
