package com.school.management.repository;

import com.school.management.persistance.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, Long> {

    List<SessionEntity> findByGroup_Id(Long groupId);
    List<SessionEntity> findByTeacher_Id(Long teacherId);
    List<SessionEntity> findByRoom_Id(Long roomId);
    List<SessionEntity> findBySessionTimeStartBetween(Date startTime, Date endTime);
    List<SessionEntity> findBySessionTimeEndBetween(Date startTime, Date endTime);

    List<SessionEntity> findByGroup_IdAndTeacher_Id(Long groupId, Long teacherId);

    List<SessionEntity> findByGroup_IdAndRoom_Id(Long groupId, Long roomId);

    List<SessionEntity> findByTeacher_IdAndRoom_Id(Long teacherId, Long roomId);

    List<SessionEntity> findByGroup_IdAndSessionTimeStartBetween(Long groupId, Date startTime, Date endTime);

    List<SessionEntity> findByTeacher_IdAndSessionTimeStartBetween(Long teacherId, Date startTime, Date endTime);

    List<SessionEntity> findByRoom_IdAndSessionTimeStartBetween(Long roomId, Date startTime, Date endTime);

    List<SessionEntity> findByGroup_IdAndTeacher_IdAndRoom_Id(Long groupId, Long teacherId, Long roomId);

    List<SessionEntity> findByGroup_IdAndTeacher_IdAndSessionTimeStartBetween(Long groupId, Long teacherId, Date startTime, Date endTime);

    List<SessionEntity> findByGroup_IdAndRoom_IdAndSessionTimeStartBetween(Long groupId, Long roomId, Date startTime, Date endTime);

    List<SessionEntity> findByTeacher_IdAndRoom_IdAndSessionTimeStartBetween(Long teacherId, Long roomId, Date startTime, Date endTime);

    List<SessionEntity> findByGroup_IdAndTeacher_IdAndRoom_IdAndSessionTimeStartBetween(Long groupId, Long teacherId, Long roomId, Date startTime, Date endTime);

    List<SessionEntity> findByGroup_IdAndTeacher_IdAndRoom_IdAndSessionTimeStartBetweenAndSessionTimeEndBetween(Long groupId, Long teacherId, Long roomId, Date startTime, Date endTime, Date startTime2, Date endTime2);
    // Add other custom methods if needed
}
