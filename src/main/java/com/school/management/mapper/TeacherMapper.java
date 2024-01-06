package com.school.management.mapper;

import com.school.management.dto.TeacherDTO;
import com.school.management.persistance.TeacherEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TeacherMapper {

    TeacherDTO teacherToTeacherDTO(TeacherEntity teacher);

    TeacherEntity teacherDTOToTeacher(TeacherDTO teacherDto);
}
