package com.school.management.mapper;

import com.school.management.config.ImageUrlService;
import com.school.management.dto.TeacherDTO;
import com.school.management.persistance.TeacherEntity;
import com.school.management.persistance.GroupEntity;
import com.school.management.dto.GroupDTO;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", uses = {GroupMapper.class})
public interface TeacherMapper {


    TeacherDTO teacherToTeacherDTO(TeacherEntity teacher);

    @Mapping(source = "firstName", target = "firstName")
    @Mapping(source = "lastName", target = "lastName")
    TeacherEntity teacherDTOToTeacher(TeacherDTO teacherDto);


    GroupDTO groupEntityToGroupDTO(GroupEntity groupEntity);
    GroupEntity groupDTOToGroupEntity(GroupDTO groupDto);


    default Set<GroupDTO> groupEntitiesToGroupDTOs(Set<GroupEntity> groups) {
        if (groups == null) {
            return null;
        }
        return groups.stream()
                .map(this::groupEntityToGroupDTO)
                .collect(Collectors.toSet());
    }

    // Use this method to map the set of GroupDTO to a set of GroupEntity
    default Set<GroupEntity> groupDTOsToGroupEntities(Set<GroupDTO> groupDTOs) {
        if (groupDTOs == null) {
            return null;
        }
        return groupDTOs.stream()
                .map(this::groupDTOToGroupEntity)
                .collect(Collectors.toSet());
    }

    @AfterMapping
    default void setPhotoUrl(TeacherEntity teacher, @MappingTarget TeacherDTO dto) {
        if (teacher.getPhoto() != null) {
            ImageUrlService imageUrlService = ApplicationContextProvider.getBean(ImageUrlService.class);
            String photoUrl = imageUrlService.getPhotoUrl(teacher.getPhoto());
            dto.setPhoto(photoUrl);
        }
    }

    // Define other mappings if needed
}
