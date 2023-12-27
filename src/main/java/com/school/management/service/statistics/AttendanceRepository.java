package com.school.management.service.statistics;

import com.school.management.persistance.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<SessionEntity, Long> {
}