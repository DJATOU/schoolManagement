package com.school.management.service;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.service.exception.CustomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.validation.ValidationException;
import java.util.List;
import java.util.Optional;

@Service
public class StudentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentService.class);

    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;

    @Autowired
    public StudentService(StudentRepository studentRepository, GroupRepository groupRepository) {
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
    }

    @Transactional(readOnly = true)
    public Optional<StudentEntity> findById(Long id) {
        try {
            return studentRepository.findById(id);
        } catch (DataAccessException e) {
            String errorMessage = "Error fetching student with ID " + id;
            LOGGER.error(errorMessage, e);
            throw new CustomServiceException(errorMessage, e);
        }
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findAll() {
        LOGGER.info("Fetching all students....");
        return studentRepository.findAll();
    }

    @Transactional
    public StudentEntity save(StudentEntity student) {
        //validateStudentData(student);
        return studentRepository.save(student);
    }

   /* private void validateStudentData(StudentEntity student) {
        if (StringUtils.isBlank(student.getFirstName()) || StringUtils.isEmpty(student.getLastName())) {
            throw new ValidationException("Student's first and last name are required");
        }
    }*/

    @Transactional
    public void delete(Long id) {
        studentRepository.deleteById(id);
    }

    @Transactional
    public void update(Long id) {
        studentRepository.updateById(id);
    }

    public void updateStudentScore(Long studentId, Double newScore) {
        StudentEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomServiceException("Student not found"));

        // Business logic
        student.setAverageScore(newScore);
        studentRepository.save(student);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByLastName(String lastName) {
        return studentRepository.findByLastName(lastName);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByFirstNameAndLastName(String firstName, String lastName) {
        return studentRepository.findByFirstNameAndLastName(firstName, lastName);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByGroupsId(Long groupId) {
        return studentRepository.findByGroups_Id(groupId);
    }

    @Transactional(readOnly = true)
    public long countByGroupsId(Long groupId) {
        return studentRepository.countByGroups_Id(groupId);
    }

    @Transactional(readOnly = true)
    public long countByLevel(String level) {
        return studentRepository.countByLevel(level);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByLevel(String level) {
        return studentRepository.findByLevel(level);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByTutorId(Long tutorId) {
        return studentRepository.findByTutorId(tutorId);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByEstablishment(String establishment) {
        return studentRepository.findByEstablishment(establishment);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByFirstNameContainingOrLastNameContaining(String firstName, String lastName) {
        return studentRepository.findByFirstNameContainingOrLastNameContaining(firstName, lastName);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByAverageScoreBetween(Double minScore, Double maxScore) {
        return studentRepository.findByAverageScoreBetween(minScore, maxScore);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByActive(Boolean active) {
        return studentRepository.findByActive(active);
    }

    public List<GroupEntity> getGroupsByStudentId(Long studentId) {
        // Utilise la méthode du repository pour récupérer les groupes
        return studentRepository.findGroupsByStudentId(studentId);
    }


    // Additional methods for business logic...
}
