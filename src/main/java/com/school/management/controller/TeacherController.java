package com.school.management.controller;

import com.school.management.persistance.TeacherEntity;
import com.school.management.service.TeacherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private final TeacherService teacherService;

    @Autowired
    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping
    public ResponseEntity<List<TeacherEntity>> getAllTeachers() {
        return ResponseEntity.ok(teacherService.getAllTeachers());
    }

    @GetMapping("/lastname/{lastName}")
    public ResponseEntity<List<TeacherEntity>> getTeachersByLastName(@PathVariable String lastName) {
        return ResponseEntity.ok(teacherService.findByLastName(lastName));
    }

    @GetMapping("/fullname")
    public ResponseEntity<List<TeacherEntity>> getTeachersByFullName(@RequestParam String firstName, @RequestParam String lastName) {
        return ResponseEntity.ok(teacherService.findByFirstNameAndLastName(firstName, lastName));
    }

    @GetMapping("/group/{groupId}")
    public ResponseEntity<List<TeacherEntity>> getTeachersByGroupId(@PathVariable Long groupId) {
        return ResponseEntity.ok(teacherService.findByGroupsId(groupId));
    }

    @PostMapping
    public ResponseEntity<TeacherEntity> createTeacher(@RequestBody TeacherEntity teacher) {

        return ResponseEntity.ok(teacherService.createTeacher(teacher));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeacherEntity> updateTeacher(@PathVariable Long id, @RequestBody TeacherEntity teacher) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, teacher));
    }

}

