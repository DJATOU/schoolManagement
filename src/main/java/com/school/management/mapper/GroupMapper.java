package com.school.management.mapper;

import com.school.management.dto.GroupDTO;
import com.school.management.persistance.*;
import com.school.management.repository.*;
import com.school.management.service.exception.CustomServiceException;
import org.mapstruct.*;

@Mapper(componentModel = "spring",  builder = @Builder())
public interface GroupMapper {

    @Mapping(source = "groupType.id", target = "groupTypeId")
    @Mapping(source = "level.id", target = "levelId")
    @Mapping(source = "subject.id", target = "subjectId")
    @Mapping(source = "price.id", target = "priceId")
    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "teacher.firstName", target = "teacherName")
    @Mapping(source = "groupType.name", target = "groupTypeName")  // Map group type name
    @Mapping(source = "level.name", target = "levelName")          // Map level name
    @Mapping(source = "subject.name", target = "subjectName")      // Map subject name
    @Mapping(source = "price.price", target = "priceAmount")      // Map price amount
    GroupDTO groupToGroupDTO(GroupEntity group);

    @Mapping(source = "groupTypeId", target = "groupType", qualifiedByName = "idToGroupType")
    @Mapping(source = "levelId", target = "level", qualifiedByName = "idToLevel")
    @Mapping(source = "subjectId", target = "subject", qualifiedByName = "idToSubject")
    @Mapping(source = "priceId", target = "price", qualifiedByName = "idToPricing")
    @Mapping(source = "teacherId", target = "teacher", qualifiedByName = "idToTeacher")
    GroupEntity groupDTOToGroup(GroupDTO groupDto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateGroupFromDto(GroupDTO dto, @MappingTarget GroupEntity entity);

    @Named("idToGroupType")
    default GroupTypeEntity idToGroupType(Long id) {
        if (id == null) {
            return null;
        }
        GroupTypeRepository groupTypeRepository = ApplicationContextProvider.getBean(GroupTypeRepository.class);
        return groupTypeRepository.findById(id)
                .orElseThrow(() -> new CustomServiceException("GroupType not found with id: " + id));
    }


    @Named("idToLevel")
    default LevelEntity idToLevel(Long id) {
        if (id == null) return null;
        LevelRepository levelRepository = ApplicationContextProvider.getBean(LevelRepository.class);
        return levelRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("Level not found with id: " + id));
    }

    @Named("idToSubject")
    default SubjectEntity idToSubject(Long id) {
        if (id == null) return null;
        SubjectRepository subjectRepository = ApplicationContextProvider.getBean(SubjectRepository.class);
        return subjectRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("Subject not found with id: " + id));
    }

    @Named("idToPricing")
    default PricingEntity idToPricing(Long id) {
        if (id == null) return null;
        PricingRepository pricingRepository = ApplicationContextProvider.getBean(PricingRepository.class);
        return pricingRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("Pricing not found with id: " + id));
    }

    @Named("idToTeacher")
    default TeacherEntity idToTeacher(Long id) {
        if (id == null) return null;
        TeacherRepository teacherRepository = ApplicationContextProvider.getBean(TeacherRepository.class);
        return teacherRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("Teacher not found with id: " + id));
    }

}