package com.school.management.mapper;

import com.school.management.config.ImageUrlService;
import com.school.management.dto.StudentDTO;
import com.school.management.persistance.LevelEntity;
import com.school.management.persistance.StudentEntity;
import java.util.Collections;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.TutorEntity;
import com.school.management.repository.LevelRepository;
import com.school.management.repository.TutorRepository;
import com.school.management.service.exception.CustomServiceException;
import org.mapstruct.*;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false), imports = {LevelRepository.class})
public interface StudentMapper {

    @Mapping(source = "tutor.id", target = "tutorId")
    @Mapping(source = "groups", target = "groupIds", qualifiedByName = "groupSetToIdSet")
    @Mapping(source = "id", target = "id")
    @Mapping(source = "level.id", target = "levelId")
    StudentDTO studentToStudentDTO(StudentEntity student);

    @Mapping(source = "tutorId", target = "tutor", qualifiedByName = "idToTutor")
    @Mapping(target = "groups", ignore = true) // Ignore groups in DTO to Entity conversion
    @Mapping(target = "attendances", ignore = true) // Ignore attendances in DTO to Entity conversion
    @Mapping(source = "levelId", target = "level", qualifiedByName="loadLevelEntity") // Utiliser une méthode pour charger le LevelEntity
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

    @AfterMapping
    default void setPhotoUrl(StudentEntity student, @MappingTarget StudentDTO dto) {
        ImageUrlService imageUrlService = ApplicationContextProvider.getBean(ImageUrlService.class);
        String photoUrl = imageUrlService.getPhotoUrl(student.getPhoto());
        dto.setPhoto(photoUrl);
    }

    @Named("loadLevelEntity")
    default LevelEntity loadLevelEntity(Long id) {
        if (id == null) return null;
        LevelRepository levelRepository = ApplicationContextProvider.getBean(LevelRepository.class);
        return levelRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("Level not found with id: " + id));
    }

}