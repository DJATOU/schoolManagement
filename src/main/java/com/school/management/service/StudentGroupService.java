package com.school.management.service;

import com.school.management.dto.StudentDTO;
import com.school.management.dto.StudentGroupDTO;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.persistance.StudentGroupEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.StudentGroupRepository;
import com.school.management.repository.StudentRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentGroupService {

    private final StudentGroupRepository studentGroupRepository;
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;

    @Autowired
    public StudentGroupService(StudentGroupRepository studentGroupRepository,
                               StudentRepository studentRepository,
                               GroupRepository groupRepository) {
        this.studentGroupRepository = studentGroupRepository;
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
    }
    @Transactional
    public void manageStudentGroupAssociations(StudentGroupDTO studentGroupDto) {
        if (studentGroupDto.isAddingStudentToGroups()) {
            addGroupsToStudent(studentGroupDto);
        } else if (studentGroupDto.isAddingStudentsToGroup()) {
            addStudentsToGroup(studentGroupDto);
        } else {
            throw new IllegalArgumentException("Invalid student group association data");
        }
    }


    public void addGroupsToStudent(StudentGroupDTO studentGroupDto) {
        StudentEntity student = studentRepository.findById(studentGroupDto.getStudentId())
                .orElseThrow(() -> new EntityNotFoundException("Student not found with id: " + studentGroupDto.getStudentId()));

        List<GroupEntity> groups = groupRepository.findAllById(studentGroupDto.getGroupIds());
        if (groups.size() != studentGroupDto.getGroupIds().size()) {
            throw new EntityNotFoundException("One or more groups not found");
        }

        groups.forEach(group -> {
            StudentGroupEntity studentGroup = StudentGroupEntity.builder()
                    .student(student)
                    .group(group)
                    .dateAssigned(studentGroupDto.getDateAssigned() != null ? studentGroupDto.getDateAssigned() : new Date())
                    .createdBy(studentGroupDto.getAssignedBy()) //TODO: Change to getCurrentUsername() AUTOMATISEZ LA GESTION DES UTILISATEURS
                    .description(studentGroupDto.getDescription())
                    .build();

            studentGroupRepository.save(studentGroup);
        });
    }

    public void addStudentsToGroup(StudentGroupDTO studentGroupDto) {
        GroupEntity group = groupRepository.findById(studentGroupDto.getGroupId())
                .orElseThrow(() -> new EntityNotFoundException("Group not found with id: " + studentGroupDto.getGroupId()));

        List<StudentEntity> students = studentRepository.findAllById(studentGroupDto.getStudentIds());
        for (StudentEntity student : students) {
            StudentGroupEntity studentGroup = StudentGroupEntity.builder()
                    .student(student)
                    .group(group)
                    .dateAssigned(studentGroupDto.getDateAssigned() != null ? studentGroupDto.getDateAssigned() : new Date())
                    .createdBy(studentGroupDto.getAssignedBy())
                    .description(studentGroupDto.getDescription())
                    .build();

            studentGroupRepository.save(studentGroup);
        }
    }

    public List<StudentDTO> getStudentsByGroupId(Long groupId) {
        List<StudentGroupEntity> studentGroups = studentGroupRepository.findByGroupId(groupId);
        return studentGroups.stream()
                .map(sg -> StudentDTO.builder()
                        .id(sg.getStudent().getId())
                        .gender(sg.getStudent().getGender())
                        .lastName(sg.getStudent().getLastName())
                        .firstName(sg.getStudent().getFirstName())
                        .build())
                .toList();
    }

}
