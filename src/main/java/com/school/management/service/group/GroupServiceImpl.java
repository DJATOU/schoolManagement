package com.school.management.service.group;

import com.school.management.dto.GroupDTO;
import com.school.management.dto.SessionSeriesDto;
import com.school.management.dto.StudentDTO;
import com.school.management.mapper.GroupMapper;
import com.school.management.mapper.StudentMapper;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.persistance.StudentGroupEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.StudentGroupRepository;
import com.school.management.service.exception.CustomServiceException;
import com.school.management.service.interfaces.GroupService;
import io.swagger.v3.core.util.ReflectionUtils;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class GroupServiceImpl implements GroupService {
    private static final Logger LOGGER = LoggerFactory.getLogger(GroupServiceImpl.class);
    private static final String GROUP_NOT_FOUND = "Group not found with id: ";
    private final GroupRepository groupRepository;
    private final GroupMapper groupMapper;
    private final StudentMapper studentMapper;
    private final ModelMapper modelMapper;
    private final GroupSearchService groupSearchService;

    private final StudentGroupRepository studentGroupRepository;

    @Autowired
    public GroupServiceImpl(GroupRepository groupRepository,
                            GroupMapper groupMapper,
                            StudentMapper studentMapper,
                            ModelMapper modelMapper,
                            GroupSearchService groupSearchService, StudentGroupRepository studentGroupRepository) {
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
        this.studentMapper = studentMapper;
        this.modelMapper = modelMapper;
        this.groupSearchService = groupSearchService;
        this.studentGroupRepository = studentGroupRepository;
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
        return groupRepository.save(group);
    }

    @Transactional
    public void delete(Long id) {
        groupRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GroupDTO> searchGroupsByNameStartingWithDTO(String name) {
        List<GroupEntity> groupEntities = groupSearchService.searchGroupsByNameStartingWith(name);
        return groupEntities.stream()
                .map(groupMapper::groupToGroupDTO)
                .toList();
    }

    @Override
    public void desactivateGroup(Long id) {
        groupRepository.findById(id).ifPresent(group -> {
            group.setActive(false);
            groupRepository.save(group);
        });
    }

    @Transactional(readOnly = true)
    public List<SessionSeriesDto> getSeriesByGroupId(Long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomServiceException(GROUP_NOT_FOUND + groupId));

        return group.getSeries().stream().map(element -> modelMapper.map(element, SessionSeriesDto.class))
                .toList();
    }



   /*Override
    public GroupEntity updateGroupPartially(Long id, Map<String, Object> updates) {
        GroupEntity existingGroup = groupRepository.findById(id)
                .orElseThrow(() -> new CustomServiceException("Group not found with id: " + id));

        groupMapper.updateGroupFromDto(groupDTO, existingGroup);

        return groupRepository.save(existingGroup);
    }*/

    @Transactional(readOnly = true)
    public Long countStudentsInGroup(Long groupId) {
        GroupEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomServiceException(GROUP_NOT_FOUND + groupId));
        return (long) group.getStudents().size();
    }

    public GroupEntity getGroupWithDetails(Long groupId) {
        return groupRepository.findGroupWithDetailsById(groupId)
                .orElseThrow(() -> new RuntimeException(GROUP_NOT_FOUND + groupId));
    }

    @Transactional(readOnly = true)
    public List<StudentDTO> getActiveStudentsByGroupId(Long groupId) {
        groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomServiceException(GROUP_NOT_FOUND + groupId));
        return studentGroupRepository.findByGroupIdAndActiveTrue(groupId).stream()
                .map(studentGroup -> studentMapper.studentToStudentDTO(studentGroup.getStudent()))
                .toList();
    }
}