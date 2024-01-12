package com.school.management.mapper;

import com.school.management.dto.SessionDTO;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.RoomEntity;
import com.school.management.persistance.SessionEntity;
import com.school.management.persistance.TeacherEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.RoomRepository;
import com.school.management.repository.TeacherRepository;
import com.school.management.service.exception.CustomServiceException;
import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring",  builder = @Builder(disableBuilder = false))
public interface SessionMapper {

    @Mapping(source = "group.id", target = "groupId")
    @Mapping(source = "teacher.id", target = "teacherId")
    @Mapping(source = "room.id", target = "roomId")
    public SessionDTO sessionEntityToSessionDto(SessionEntity entity) ;

    @Mapping(source = "groupId", target = "group", qualifiedByName = "idToGroup")
    @Mapping(source = "teacherId", target = "teacher", qualifiedByName = "idToTeacher")
    @Mapping(source = "roomId", target = "room", qualifiedByName = "idToRoom")
    public SessionEntity sessionDtoToSessionEntity(SessionDTO dto);

    @Named("idToGroup")
    default GroupEntity idToGroup(Long id) {
        if (id == null) return null;
        GroupRepository groupRepository = ApplicationContextProvider.getBean(GroupRepository.class);
        return groupRepository.findById(id)
                .orElseThrow(() -> new CustomServiceException("Group not found for ID: " + id));
    }

    @Named("idToTeacher")
    default TeacherEntity idToTeacher(Long id) {
        if (id == null) return null;
        TeacherRepository teacherRepository = ApplicationContextProvider.getBean(TeacherRepository.class);
        return teacherRepository.findById(id)
                .orElseThrow(() -> new CustomServiceException("Teacher not found for ID: " + id));
    }

    @Named("idToRoom")
    default RoomEntity idToRoom(Long id) {
        if (id == null) return null;
        RoomRepository roomRepository = ApplicationContextProvider.getBean(RoomRepository.class);
        return roomRepository.findById(id)
                .orElseThrow(() -> new CustomServiceException("Room not found for ID: " + id));
    }

}
