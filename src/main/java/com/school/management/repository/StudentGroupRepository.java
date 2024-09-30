package com.school.management.repository;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.persistance.StudentGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentGroupRepository extends JpaRepository<StudentGroupEntity, Long> {

    @Query("SELECT sg FROM StudentGroupEntity sg WHERE sg.group.id = :groupId")
    List<StudentGroupEntity> findByGroupId(Long groupId);

    boolean existsByStudentAndGroup(StudentEntity student, GroupEntity group);

    Optional<StudentGroupEntity> findByGroupIdAndStudentId(Long groupId, Long studentId);

    List<StudentGroupEntity> findByGroupIdAndActiveTrue(Long groupId);

    List<StudentGroupEntity> findByStudentIdAndActiveTrue(Long studentId);
}