package com.school.management.repository;

import com.school.management.persistance.SessionEntity;
import com.school.management.persistance.SessionSeriesEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SessionRepository extends JpaRepository<SessionEntity, Long>, JpaSpecificationExecutor<SessionEntity> {

    List<SessionEntity> findByGroupId(Long groupId);


    List<SessionEntity> findBySessionSeries(SessionSeriesEntity series);
}
