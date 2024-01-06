package com.school.management.mapper;

import com.school.management.dto.AttendanceDTO;
import com.school.management.persistance.*;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.SessionRepository;
import com.school.management.repository.SessionSeriesRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.service.exception.CustomServiceException;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", builder = @Builder(disableBuilder = false))
public interface AttendanceMapper {

    @Mapping(source = "student.id", target = "studentId")
    @Mapping(source = "session.id", target = "sessionId")
    @Mapping(source = "sessionSeries.id", target = "sessionSeriesId")
    @Mapping(source = "group.id", target = "groupId")
    AttendanceDTO attendanceToAttendanceDTO(AttendanceEntity attendance);

    @Mapping(source = "studentId", target = "student", qualifiedByName = "idToStudent")
    @Mapping(source = "sessionId", target = "session", qualifiedByName = "idToSession")
    @Mapping(source = "sessionSeriesId", target = "sessionSeries", qualifiedByName = "idToSessionSeries")
    @Mapping(source = "groupId", target = "group", qualifiedByName = "idToGroup")
    AttendanceEntity attendanceDTOToAttendance(AttendanceDTO attendanceDTO);

    @Named("idToStudent")
    default StudentEntity idToStudent(Long id) {
        if (id == null) return null ;
        StudentRepository studentRepository = ApplicationContextProvider.getBean(StudentRepository.class);
         return studentRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("Student not found with id: " + id));

    }

    @Named("idToSession")
    default SessionEntity idToSession(Long id) {
        if (id == null) return null;
        SessionRepository sessionRepository = ApplicationContextProvider.getBean(SessionRepository.class);
        return sessionRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("Session not found with id: " + id));

    }

    @Named("idToSessionSeries")
    default SessionSeriesEntity idToSessionSeries(Long id) {
        if (id == null) return null;
        SessionSeriesRepository sessionSeriesRepository = ApplicationContextProvider.getBean(SessionSeriesRepository.class);
        return sessionSeriesRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("SessionSeries not found with id: " + id));
    }

    @Named("idToGroup")
    default GroupEntity idToGroup(Long id) {
        if (id == null) return null;
        GroupRepository groupRepository = ApplicationContextProvider.getBean(GroupRepository.class);
        return groupRepository.findById(id).orElseThrow(() ->
                new CustomServiceException("Group not found with id: " + id));
    }
}
