package com.school.management.controller;

import com.school.management.dto.StudentDTO;
import com.school.management.dto.StudentGroupDTO;
import com.school.management.service.StudentGroupService;
import com.school.management.service.exception.GroupAlreadyAssociatedException;
import jakarta.persistence.EntityNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/student-groups")
public class StudentGroupController {

    private final StudentGroupService studentGroupService;
    private static final Logger logger = LoggerFactory.getLogger(StudentGroupController.class);
    @Autowired
    public StudentGroupController(StudentGroupService studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @PostMapping("/{studentId}/addGroups")
    public ResponseEntity<Map<String, Object>> addGroupsToStudent(@PathVariable Long studentId,
                                                                  @RequestBody StudentGroupDTO studentGroupDto) {
        logger.info("Received request to add groups to student: {}", studentId);
        logger.info("StudentGroupDTO: {}", studentGroupDto);
        try {
            studentGroupDto.setStudentId(studentId);
            studentGroupService.manageStudentGroupAssociations(studentGroupDto);
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Groups added to student successfully");
            return ResponseEntity.ok(response);
        } catch (GroupAlreadyAssociatedException e) {
            logger.error("GroupAlreadyAssociatedException: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Some groups were already associated with the student");
            response.put("alreadyAssociatedGroups", e.getGroupNames());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
        } catch (EntityNotFoundException e) {
            logger.error("EntityNotFoundException: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception e) {
            logger.error("Exception: {}", e.getMessage());
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Error adding groups to student: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }


    @PostMapping("/{groupId}/addStudents")
    public ResponseEntity<String> addStudentsToGroup(@PathVariable Long groupId,
                                                     @RequestBody StudentGroupDTO studentGroupDto) {
        studentGroupDto.setGroupId(groupId);
        studentGroupService.manageStudentGroupAssociations(studentGroupDto);
        return ResponseEntity.ok("Students added to group successfully");
    }

    @GetMapping("/{groupId}/students")
    public ResponseEntity<List<StudentDTO>> getStudentsOfGroup(@PathVariable Long groupId) {
        List<StudentDTO> students = studentGroupService.getStudentsByGroupId(groupId);
        return ResponseEntity.ok(students);
    }
}
