package com.school.management.mapper;

import com.school.management.dto.SessionSeriesDto;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.SessionSeriesEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.service.exception.CustomServiceException;
import org.mapstruct.*;

// SessionSeriesMapper.java

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = MappingConstants.ComponentModel.SPRING)
public interface SessionSeriesMapper {

    @Mapping(source = "groupId", target = "group", qualifiedByName = "idToGroup")
    SessionSeriesEntity toEntity(SessionSeriesDto sessionSeriesDto);

    @Mapping(source = "group.id", target = "groupId")
    @Mapping(target = "numberOfSessionsCreated", expression = "java(getNumberOfSessionsCreated(sessionSeriesEntity))")
    SessionSeriesDto toDto(SessionSeriesEntity sessionSeriesEntity);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    SessionSeriesEntity partialUpdate(SessionSeriesDto sessionSeriesDto, @MappingTarget SessionSeriesEntity sessionSeriesEntity);

    @Named("idToGroup")
    default GroupEntity idToGroup(Long id) {
        System.out.println("Mapping groupId: " + id);
        if (id == null) return null;
        GroupRepository groupRepository = ApplicationContextProvider.getBean(GroupRepository.class);
        return groupRepository.findById(id)
                .orElseThrow(() -> new CustomServiceException("Group not found for ID: " + id));
    }

    // Ajouter cette méthode par défaut pour calculer numberOfSessionsCreated
    default int getNumberOfSessionsCreated(SessionSeriesEntity sessionSeriesEntity) {
        if (sessionSeriesEntity.getSessions() != null) {
            return sessionSeriesEntity.getSessions().size();
        } else {
            return 0;
        }
    }
}
