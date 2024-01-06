package com.school.management.repository;

import com.school.management.persistance.SessionSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionSeriesRepository extends JpaRepository<SessionSeriesEntity, Long> {
     List<SessionSeriesEntity> findByStudentId(Long studentId);

}
