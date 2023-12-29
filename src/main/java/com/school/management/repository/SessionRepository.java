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

    // Add other custom methods if needed
}
