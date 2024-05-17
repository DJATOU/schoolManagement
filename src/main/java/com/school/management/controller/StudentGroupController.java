package com.school.management.controller;

import com.school.management.dto.StudentDTO;
import com.school.management.dto.StudentGroupDTO;
import com.school.management.service.StudentGroupService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/student-groups")
public class StudentGroupController {

    private final StudentGroupService studentGroupService;

    @Autowired
    public StudentGroupController(StudentGroupService studentGroupService) {
        this.studentGroupService = studentGroupService;
    }

    @PostMapping("/{studentId}/addGroups")
    public ResponseEntity<String> addGroupsToStudent(@PathVariable Long studentId,
                                                     @RequestBody StudentGroupDTO studentGroupDto) {
        try {
            studentGroupDto.setStudentId(studentId);
            studentGroupService.manageStudentGroupAssociations(studentGroupDto);
            return ResponseEntity.ok("Groups added to student successfully");
        } catch (EntityNotFoundException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error adding groups to student: " + e.getMessage());
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
