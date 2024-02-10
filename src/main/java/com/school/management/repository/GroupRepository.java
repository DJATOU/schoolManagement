package com.school.management.repository;

import com.school.management.persistance.GroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GroupRepository extends JpaRepository<GroupEntity, Long> {

    List<GroupEntity> findByLevelId(Long levelId);
    List<GroupEntity> findBySubjectId(Long subjectId);
    List<GroupEntity> findByTeacherId(Long teacherId);
    List<GroupEntity> findByActive(Boolean active);

    List<GroupEntity> findByStudents_Id(Long studentId);
}
