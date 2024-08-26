package com.school.management.repository;

import com.school.management.persistance.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    List<StudentEntity> findByGroups_Id(Long groupId);

    List<StudentEntity> findByLevelId(Long levelId);

    List<StudentEntity> findByEstablishment(String establishment);

    // find student by last name
    List<StudentEntity> findByLastName(String lastName);

    // find student by first name and last name
    List<StudentEntity> findByFirstNameAndLastName(String firstName, String lastName);

    List<StudentEntity> findAllByActiveTrue();

}
