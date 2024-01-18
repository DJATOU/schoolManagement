package com.school.management.repository;

import com.school.management.persistance.AttendanceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttendanceRepository extends JpaRepository<AttendanceEntity, Long> {

    List<AttendanceEntity> findByStudentId(Long studentId);
    List<AttendanceEntity> findBySessionId(Long sessionId);
    List<AttendanceEntity> findByIsPresent(Boolean isPresent);
    List<AttendanceEntity> findByActive(Boolean active);

    List<AttendanceEntity> findByStudentIdAndSessionId(Long studentId, Long sessionId);

    // find present student by session id and group id
    List<AttendanceEntity> findBySessionIdAndStudent_Groups_Id(Long sessionId, Long groupId);

    long countByStudentIdAndIsPresent(Long studentId, Boolean isPresent);

    long countByStudentIdAndSessionSeriesIdAndIsPresent(Long studentId, Long seriesId, boolean isPresent);


}
