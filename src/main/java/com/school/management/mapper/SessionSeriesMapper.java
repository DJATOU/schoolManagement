package com.school.management.mapper;

import com.school.management.dto.SessionSeriesDto;
import com.school.management.persistance.SessionSeriesEntity;
import org.mapstruct.*;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SessionSeriesMapper {
    SessionSeriesEntity toEntity(SessionSeriesDto sessionSeriesDto);

    SessionSeriesDto toDto(SessionSeriesEntity sessionSeriesEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SessionSeriesEntity partialUpdate(SessionSeriesDto sessionSeriesDto, @MappingTarget SessionSeriesEntity sessionSeriesEntity);
}