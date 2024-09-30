package com.school.management.service.student;

import com.school.management.persistance.StudentEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class StudentSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional(readOnly = true)
    public List<StudentEntity> searchStudentsByNameStartingWith(String input) {
        return getStudentEntities(input, entityManager);
    }

    static List<StudentEntity> getStudentEntities(String input, EntityManager entityManager) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<StudentEntity> cq = cb.createQuery(StudentEntity.class);
        Root<StudentEntity> student = cq.from(StudentEntity.class);

        // Normalize and split input by spaces
        String[] nameParts = input.trim().toLowerCase().split("\\s+");
        Predicate finalPredicate;

        if (nameParts.length == 1) {
            String pattern = nameParts[0] + "%";
            // Search either first or last name if only one name part is provided
            Predicate firstNamePredicate = cb.like(cb.lower(student.get("firstName")), pattern);
            Predicate lastNamePredicate = cb.like(cb.lower(student.get("lastName")), pattern);
            finalPredicate = cb.or(firstNamePredicate, lastNamePredicate);
        } else {
            // Assume the first part is the first name and the second part is the last name for simplicity
            String firstNamePattern = nameParts[0] + "%";
            String lastNamePattern = nameParts[1] + "%";
            Predicate firstNamePredicate = cb.like(cb.lower(student.get("firstName")), firstNamePattern);
            Predicate lastNamePredicate = cb.like(cb.lower(student.get("lastName")), lastNamePattern);

            // Apply both conditions
            finalPredicate = cb.and(firstNamePredicate, lastNamePredicate);
        }

        cq.where(finalPredicate);

        // Execute the query
        TypedQuery<StudentEntity> query = entityManager.createQuery(cq);
        return query.getResultList();
    }
}