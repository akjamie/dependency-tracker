package org.akj.test.tracker.application.rule.mapper;

import org.akj.test.tracker.application.rule.dto.ComplianceDto;
import org.akj.test.tracker.application.rule.dto.EverGreenRuleDto;
import org.akj.test.tracker.domain.rule.model.Compliance;
import org.akj.test.tracker.domain.rule.model.EverGreenRule;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.WARN)
public interface EverGreenRuleMapstructMapper {
    EverGreenRuleMapstructMapper INSTANCE = Mappers.getMapper(EverGreenRuleMapstructMapper.class);

    EverGreenRule toDomain(EverGreenRuleDto evergreenRuleDto);

    EverGreenRuleDto toDto(EverGreenRule evergreenRule);

    Compliance toDomain(ComplianceDto complianceDto);

    ComplianceDto toDto(Compliance compliance);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "createdBy", ignore = true)
    @Mapping(target = "checksum", ignore = true)
    @Mapping(target = "updatedBy", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateDomainFromDto(EverGreenRuleDto dto, @MappingTarget EverGreenRule domain);
}
