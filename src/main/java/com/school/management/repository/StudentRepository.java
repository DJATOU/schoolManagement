package com.school.management.repository;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    List<StudentEntity> findByGroups_Id(Long groupId);

    long countByGroups_Id(Long groupId);

    long countByLevel(String level);

    List<StudentEntity> findByLevel(String level);

    List<StudentEntity> findByTutorId(Long tutorId);

    List<StudentEntity> findByEstablishment(String establishment);

    List<StudentEntity> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName);

    List<StudentEntity> findByAverageScoreBetween(Double minScore, Double maxScore);

    List<StudentEntity> findByActive(Boolean active);

    @Query("SELECT s FROM StudentEntity s JOIN s.groups g WHERE g.subject.id = :subjectId")
    List<StudentEntity> findStudentsBySubjectId(Long subjectId);

    @Query("SELECT COUNT(s) FROM StudentEntity s JOIN s.groups g WHERE g.subject.id = :subjectId")
    long countStudentsBySubjectId(Long subjectId);

    @Query("SELECT COUNT(s) FROM StudentEntity s JOIN s.groups g WHERE g.subject.id = :subjectId AND s.averageScore >= :minScore AND s.averageScore <= :maxScore")
    long countStudentsBySubjectIdAndAverageScoreBetween(Long subjectId, Double minScore, Double maxScore);

    @Query("SELECT AVG(s.averageScore) FROM StudentEntity s JOIN s.groups g WHERE g.subject.id = :subjectId")
    Double averageScoreBySubjectId(Long subjectId);

    // find student start with letter
    @Query("SELECT s FROM StudentEntity s WHERE s.firstName LIKE :letter%")
    List<StudentEntity> findStudentByLetter(String letter);

    @Modifying
    @Query("UPDATE StudentEntity s SET s.active = false WHERE s.id = :id")
    void updateById(Long id);
    // find student by age
    @Query("SELECT s FROM StudentEntity s WHERE s.dateOfBirth BETWEEN :minDate AND :maxDate")
    List<StudentEntity> findStudentByAge(String minDate, String maxDate);

    // find student by last name
    List<StudentEntity> findByLastName(String lastName);

    // find student by first name and last name
    List<StudentEntity> findByFirstNameAndLastName(String firstName, String lastName);

    @Query("SELECT g FROM GroupEntity g JOIN g.students s WHERE s.id = :studentId")
    List<GroupEntity> findGroupsByStudentId(Long studentId);

}
