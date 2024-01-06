package com.school.management.service;

import com.school.management.persistance.GroupEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.service.exception.CustomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class GroupService {

    private static final Logger LOGGER = LoggerFactory.getLogger(GroupService.class);
    private final GroupRepository groupRepository;

    @Autowired
    public GroupService(GroupRepository groupRepository) {
        this.groupRepository = groupRepository;
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

    // Additional methods and business logic as needed...

    // For example, a method to update group details, etc.
}
