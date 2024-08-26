package com.school.management.service;

import com.school.management.dto.StudentDTO;
import com.school.management.mapper.StudentMapper;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.repository.StudentRepository;
import com.school.management.service.exception.CustomServiceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class StudentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentService.class);

    private static final String LASTNAME = "lastName";

    private static final String FIRSTNAME = "firstName";
    @PersistenceContext
    private EntityManager entityManager;
    private final StudentRepository studentRepository;
    private static final String basePhotoUrl = "http://localhost:8080/personne/";
    private StudentMapper studentMapper;

    @Autowired
    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper){
        this.studentMapper = studentMapper;
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
    public List<StudentEntity> searchStudents(String firstName, String lastName, Long level, Long groupId, String establishment) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StudentEntity> cq = cb.createQuery(StudentEntity.class);
        Root<StudentEntity> student = cq.from(StudentEntity.class);

        Predicate[] predicates = Stream.of(
                        buildPredicate(firstName, name -> cb.equal(cb.lower(student.get(FIRSTNAME)), name.toLowerCase())),
                        buildPredicate(lastName, name -> cb.equal(cb.lower(student.get(LASTNAME)), name.toLowerCase())),
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

    @Transactional(readOnly = true)
    public List<StudentEntity> searchStudentsByNameStartingWith(String input) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StudentEntity> cq = cb.createQuery(StudentEntity.class);
        Root<StudentEntity> student = cq.from(StudentEntity.class);

        // Normalize and split input by spaces
        String[] nameParts = input.trim().toLowerCase().split("\\s+");
        Predicate finalPredicate;

        if (nameParts.length == 1) {
            String pattern = nameParts[0] + "%";
            // Search either first or last name if only one name part is provided
            Predicate firstNamePredicate = cb.like(cb.lower(student.get(FIRSTNAME)), pattern);
            Predicate lastNamePredicate = cb.like(cb.lower(student.get(LASTNAME)), pattern);
            finalPredicate = cb.or(firstNamePredicate, lastNamePredicate);
        } else {
            // Assume the first part is the first name and the second part is the last name for simplicity
            String firstNamePattern = nameParts[0] + "%";
            String lastNamePattern = nameParts[1] + "%";
            Predicate firstNamePredicate = cb.like(cb.lower(student.get(FIRSTNAME)), firstNamePattern);
            Predicate lastNamePredicate = cb.like(cb.lower(student.get(LASTNAME)), lastNamePattern);

            // Apply both conditions
            finalPredicate = cb.and(firstNamePredicate, lastNamePredicate);
        }

        cq.where(finalPredicate);

        // Execute the query
        TypedQuery<StudentEntity> query = entityManager.createQuery(cq);
        return query.getResultList();
    }


    @Transactional(readOnly = true)
    public List<StudentDTO> searchStudentsByNameStartingWithDTO(String name) {
        List<StudentEntity> studentEntities = searchStudentsByNameStartingWith(name);
        return studentEntities.stream()
                .map(entity -> {
                    StudentDTO dto = studentMapper.studentToStudentDTO(entity);
                    if (entity.getPhoto() != null && !entity.getPhoto().isEmpty()) {
                        Path photoPath = Paths.get(entity.getPhoto());
                        String photoName = photoPath.getFileName().toString();
                        String photoUrl = basePhotoUrl + photoName;
                        dto.setPhoto(photoUrl);
                    }
                    return dto;
                })
                .toList();
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
    public List<StudentEntity> findByLevel(Long level) {
        return studentRepository.findByLevelId(level);
    }

    @Transactional(readOnly = true)
    public List<StudentEntity> findByEstablishment(String establishment) {
        return studentRepository.findByEstablishment(establishment);
    }

    public List<StudentEntity> findAllActiveStudents() {
        return studentRepository.findAllByActiveTrue();
    }
}
