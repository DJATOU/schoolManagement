package com.school.management.service.student;

import com.school.management.dto.StudentDTO;
import com.school.management.mapper.StudentMapper;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
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

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

@Service
public class StudentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(StudentService.class);
    private static final String LASTNAME = "lastName";
    private static final String FIRSTNAME = "firstName";
    @PersistenceContext
    private EntityManager entityManager;
    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;
    private final StudentSearchService studentSearchService;

    @Autowired
    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper, StudentSearchService studentSearchService){
        this.studentMapper = studentMapper;
        this.studentRepository = studentRepository;
        this.studentSearchService = studentSearchService;
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
    public List<StudentDTO> searchStudentsByNameStartingWithDTO(String name) {
        List<StudentEntity> studentEntities = studentSearchService.searchStudentsByNameStartingWith(name);
        return studentEntities.stream()
                .map(entity -> {
                    StudentDTO dto = studentMapper.studentToStudentDTO(entity);
                    if (entity.getPhoto() != null && !entity.getPhoto().isEmpty()) {
                        String fileName = Paths.get(entity.getPhoto()).getFileName().toString();
                        String photoUrl = "http://localhost:8080/api/students/photos/" + fileName;
                        dto.setPhoto(photoUrl);
                    } else {
                        // Optionnel : définir une image par défaut
                        dto.setPhoto("assets/default-avatar.png");
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

    @Transactional
    public void desactivateStudent(Long id) {
        StudentEntity student = studentRepository.findById(id)
                .orElseThrow(() -> new CustomServiceException("Student not found with id " + id));
        student.setActive(false);
        studentRepository.save(student);
    }
}