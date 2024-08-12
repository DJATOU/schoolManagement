package com.school.management.service;

import com.school.management.dto.TeacherDTO;
import com.school.management.mapper.TeacherMapper;
import com.school.management.persistance.TeacherEntity;
import com.school.management.repository.TeacherRepository;
import com.school.management.service.exception.CustomServiceException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

@Service
public class TeacherService {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherService.class);

    @PersistenceContext
    private EntityManager entityManager;
    private final TeacherRepository teacherRepository;
    private final String BASE_PHOTO_URL = "http://localhost:8080/personne/";

    private TeacherMapper teacherMapper;
    @Autowired
    public TeacherService(TeacherRepository teacherRepository, TeacherMapper teacherMapper) {
        this.teacherRepository = teacherRepository;
        this.teacherMapper = teacherMapper;
    }

    @Transactional
    public TeacherEntity save(TeacherEntity teacher) {
        return teacherRepository.save(teacher);
    }

    public List<TeacherEntity> getAllTeachers() {
        LOGGER.info("Fetching all teachers...");
        return teacherRepository.findAll();
    }

    public List<TeacherEntity> findByLastName(String lastName) {
        return teacherRepository.findByLastName(lastName);
    }

    public List<TeacherEntity> findByFirstNameAndLastName(String firstName, String lastName) {
        return teacherRepository.findByFirstNameAndLastName(firstName, lastName);
    }

    public List<TeacherEntity> findByGroupsId(Long groupId) {
        return teacherRepository.findByGroups_Id(groupId);
    }

    public TeacherEntity createTeacher(TeacherEntity teacher) {
        return teacherRepository.save(teacher);
    }

    public TeacherEntity updateTeacher(Long id, TeacherEntity teacher) {
        TeacherEntity teacherToUpdate = teacherRepository.findById(id).orElseThrow();
        teacherToUpdate.setFirstName(teacher.getFirstName());
        teacherToUpdate.setLastName(teacher.getLastName());
        teacherToUpdate.setGroups(teacher.getGroups());
        return teacherRepository.save(teacherToUpdate);
    }

    @Transactional(readOnly = true)
    public List<TeacherDTO> searchTeachersByNameStartingWithDTO(String name) {
        List<TeacherEntity> teacherEntities = searchTeachersByNameStartingWith(name);
        return teacherEntities.stream()
                .map(entity -> {
                    TeacherDTO dto = teacherMapper.teacherToTeacherDTO(entity);
                    if (entity.getPhoto() != null && !entity.getPhoto().isEmpty()) {
                        Path photoPath = Paths.get(entity.getPhoto());
                        String photoName = photoPath.getFileName().toString();
                        String photoUrl = BASE_PHOTO_URL + photoName;
                        dto.setPhoto(photoUrl);
                    }
                    return dto;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherEntity> searchTeachersByNameStartingWith(String input) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<TeacherEntity> cq = cb.createQuery(TeacherEntity.class);
        Root<TeacherEntity> teacher = cq.from(TeacherEntity.class);

        String[] nameParts = input.trim().toLowerCase().split("\\s+");
        Predicate finalPredicate;

        if (nameParts.length == 1) {
            String pattern = nameParts[0] + "%";
            Predicate firstNamePredicate = cb.like(cb.lower(teacher.get("firstName")), pattern);
            Predicate lastNamePredicate = cb.like(cb.lower(teacher.get("lastName")), pattern);
            finalPredicate = cb.or(firstNamePredicate, lastNamePredicate);
        } else {
            String firstNamePattern = nameParts[0] + "%";
            String lastNamePattern = nameParts[1] + "%";
            Predicate firstNamePredicate = cb.like(cb.lower(teacher.get("firstName")), firstNamePattern);
            Predicate lastNamePredicate = cb.like(cb.lower(teacher.get("lastName")), lastNamePattern);
            finalPredicate = cb.and(firstNamePredicate, lastNamePredicate);
        }

        cq.where(finalPredicate);

        TypedQuery<TeacherEntity> query = entityManager.createQuery(cq);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    public Optional<TeacherEntity> findById(Long id) {
        try {
            return teacherRepository.findById(id);
        } catch (DataAccessException e) {
            throw new CustomServiceException("Error fetching teacher with ID " + id, e);
        }
    }

    public void desactivateTeacher(Long id) {
        teacherRepository.findById(id).ifPresent(teacher -> {
            teacher.setActive(false);
            teacherRepository.save(teacher);
        });
    }
}

