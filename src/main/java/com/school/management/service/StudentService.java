package com.school.management.service;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.repository.StudentGroupRepository;
import com.school.management.repository.StudentRepository;
import com.school.management.service.exception.CustomServiceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class StudentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentService.class);
    @PersistenceContext
    private EntityManager entityManager;
    private final StudentRepository studentRepository;

    @Autowired
    public StudentService(StudentRepository studentRepository) {
        this.studentRepository = studentRepository;
    }

    @Transactional(readOnly = true)
    public Optional<StudentEntity> findById(Long id) {
        try {
            return studentRepository.findById(id);
        } catch (DataAccessException e) {
            throw new CustomServiceException("Error fetching student with ID " + id, e);
        }
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findAll() {
        LOGGER.info("Fetching all students....");
        return studentRepository.findAll();
    }

    @Transactional
    public StudentEntity save(StudentEntity student) {
        return studentRepository.save(student);
    }

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





    @Transactional
    public List<StudentEntity> searchStudents(String firstName, String lastName, String level, Long groupId, String establishment) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StudentEntity> cq = cb.createQuery(StudentEntity.class);
        Root<StudentEntity> student = cq.from(StudentEntity.class);

        Predicate[] predicates = Stream.of(
                        buildPredicate(firstName, name -> cb.like(cb.lower(student.get("firstName")), "%" + name.toLowerCase() + "%")),
                        buildPredicate(lastName, name -> cb.like(cb.lower(student.get("lastName")), "%" + name.toLowerCase() + "%")),
                        buildPredicate(level, lev -> cb.equal(student.get("level"), lev)),
                        buildPredicate(groupId, id -> {
                            Join<StudentEntity, GroupEntity> groupsJoin = student.join("groups");
                            return cb.equal(groupsJoin.get("id"), id);
                        }),
                        buildPredicate(establishment, est -> cb.equal(student.get("establishment"), est))
                )
                .filter(Objects::nonNull)
                .toArray(Predicate[]::new);

        cq.where(cb.and(predicates));
        return entityManager.createQuery(cq).getResultList();
    }

    private <T> Predicate buildPredicate(T value, Function<T, Predicate> predicateFunction) {
        return (value != null) ? predicateFunction.apply(value) : null;
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
    public List<StudentEntity> findByEstablishment(String establishment) {
        return studentRepository.findByEstablishment(establishment);
    }

}
