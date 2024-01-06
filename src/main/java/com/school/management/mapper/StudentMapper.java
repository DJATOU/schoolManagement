package com.school.management.mapper;

import com.school.management.dto.StudentDTO;
import com.school.management.persistance.StudentEntity;
import java.util.Collections;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.TutorEntity;
import com.school.management.repository.TutorRepository;
import com.school.management.service.exception.CustomServiceException;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface StudentMapper {

    @Mapping(source = "tutor.id", target = "tutorId")
    @Mapping(source = "groups", target = "groupIds", qualifiedByName = "groupSetToIdSet")
    StudentDTO studentToStudentDTO(StudentEntity student);

    @Mapping(source = "tutorId", target = "tutor", qualifiedByName = "idToTutor")
    @Mapping(target = "groups", ignore = true) // Ignore groups in DTO to Entity conversion
    @Mapping(target = "attendances", ignore = true) // Ignore attendances in DTO to Entity conversion
    StudentEntity studentDTOToStudent(StudentDTO studentDto);

    @Named("groupSetToIdSet")
    default Set<Long> groupSetToIdSet(Set<GroupEntity> groups) {
        if (groups == null) {
            return Collections.emptySet();
        }
        return groups.stream().map(GroupEntity::getId).collect(Collectors.toSet());
    }

    @Named("idToTutor")
    default TutorEntity idToTutor(Long id) {
        if (id == null) {
            return null;
        }
        return ApplicationContextProvider.getBean(TutorRepository.class)
                .findById(id)
                .orElseThrow(() -> new CustomServiceException("Tutor not found with id: " + id));
    }
}
