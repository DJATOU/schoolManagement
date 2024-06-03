package com.school.management.service;

import com.school.management.dto.GroupDTO;
import com.school.management.mapper.GroupMapper;
import com.school.management.persistance.GroupEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.service.exception.CustomServiceException;
import com.school.management.service.interfaces.GroupService;
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

import java.util.List;
import java.util.Optional;

@Service
public class GroupServiceImpl implements GroupService {
    @PersistenceContext
    private EntityManager entityManager;
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);
    private final GroupRepository groupRepository;

    private GroupMapper groupMapper;
    @Autowired
    public GroupServiceImpl(GroupRepository groupRepository, GroupMapper groupMapper) {
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
    }

    public List<GroupEntity> findByTeacherId(Long teacherId) {
        return groupRepository.findAll().stream()
                .filter(group -> group.getTeacher() != null && group.getTeacher().getId().equals(teacherId))
                .toList();
    }

    public List<GroupEntity> findByStudentId(Long studentId) {
        return groupRepository.findAll().stream()
                .filter(group -> group.getStudents().stream().anyMatch(student -> student.getId().equals(studentId)))
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<GroupEntity> findById(Long id) {
        try {
            return groupRepository.findById(id);
        } catch (DataAccessException e) {
            String errorMessage = "Error fetching group with ID " + id;
            throw new CustomServiceException(errorMessage, e);
        }
    }

    @Transactional(readOnly = true)
    public List<GroupEntity> findAll() {
        LOGGER.info("Fetching all groups...");
        return groupRepository.findAll();
    }

    @Transactional
    public GroupEntity save(GroupEntity group) {
        // Additional validation or business logic here if needed
        return groupRepository.save(group);
    }

    @Transactional
    public void delete(Long id) {
        groupRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupDTO> searchGroupsByNameStartingWithDTO(String name) {
        List<GroupEntity> groupEntities = searchGroupsByNameStartingWith(name);
        return groupEntities.stream()
                .map(entity -> groupMapper.groupToGroupDTO(entity))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupEntity> searchGroupsByNameStartingWith(String input) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<GroupEntity> cq = cb.createQuery(GroupEntity.class);
        Root<GroupEntity> group = cq.from(GroupEntity.class);

        String pattern = input.trim().toLowerCase() + "%";
        Predicate namePredicate = cb.like(cb.lower(group.get("name")), pattern);

        cq.where(namePredicate);

        TypedQuery<GroupEntity> query = entityManager.createQuery(cq);
        return query.getResultList();
    }
}
