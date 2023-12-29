package com.school.management.repository;

import com.school.management.persistance.TeacherEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TeacherRepository extends JpaRepository<TeacherEntity, Long> {

    // Custom methods:
    List<TeacherEntity> findByLastName(String lastName);

    List<TeacherEntity> findByFirstNameAndLastName(String firstName, String lastName);

    // Method to find teachers associated with a specific group
    List<TeacherEntity> findByGroups_Id(Long groupId);

}
