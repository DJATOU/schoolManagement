package com.school.management.controller;

import com.school.management.dto.TeacherDTO;
import com.school.management.mapper.TeacherMapper;
import com.school.management.persistance.TeacherEntity;
import com.school.management.service.TeacherService;
import com.school.management.service.exception.CustomServiceException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    @Value("${app.upload.dir}")
    private String uploadDir;
    private final TeacherService teacherService;
    private final TeacherMapper teacherMapper;

    @Autowired
    public TeacherController(TeacherService teacherService, TeacherMapper teacherMapper) {
        this.teacherService = teacherService;
        this.teacherMapper = teacherMapper;
    }

    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<List<TeacherDTO>> getAllTeachers() {
        List<TeacherDTO> teachers = teacherService.getAllTeachers().stream()
                .map(teacherMapper::teacherToTeacherDTO)
                .toList();
        return ResponseEntity.ok(teachers);
    }

    @Transactional(readOnly = true)
    @GetMapping("/id/{id}")
    public ResponseEntity<TeacherDTO> getTeacherById(@PathVariable Long id) {
        TeacherEntity teacher = teacherService.findById(id)
                .orElseThrow(() -> new CustomServiceException("Teacher not found with id " + id));
        return ResponseEntity.ok(teacherMapper.teacherToTeacherDTO(teacher));
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

    @PostMapping("/createTeacher")
    public ResponseEntity<?> createTeacher(@Valid @ModelAttribute TeacherDTO teacherDto,
                                           @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("File is required and must not be empty.");
        }
        String fileName = StringUtils.cleanPath(Objects.requireNonNull(file.getOriginalFilename()));

        try {
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            teacherDto.setPhoto(filePath.toString());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not save file: " + fileName);
        }

        try {
            TeacherEntity teacher = teacherMapper.teacherDTOToTeacher(teacherDto);
            TeacherEntity savedTeacher = teacherService.save(teacher);
            return ResponseEntity.ok(teacherMapper.teacherToTeacherDTO(savedTeacher));
        } catch (Exception e) {
            // Handle other potential exceptions during the saving of the teacher
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not save teacher");
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<TeacherEntity> updateTeacher(@PathVariable Long id, @RequestBody TeacherEntity teacher) {
        return ResponseEntity.ok(teacherService.updateTeacher(id, teacher));
    }

    @Transactional(readOnly = true)
    @GetMapping("/searchByNames")
    public ResponseEntity<List<TeacherDTO>> getTeachersByFirstNameAndOrLastName(@RequestParam(required = false) String search) {
        List<TeacherDTO> teachers = teacherService.searchTeachersByNameStartingWithDTO(search);
        return ResponseEntity.ok(teachers);
    }

    // desactivate a teacher
    @DeleteMapping("disable/{id}")
    public ResponseEntity<Boolean> desactivateTeacher(@PathVariable Long id) {
        teacherService.desactivateTeacher(id);
        return ResponseEntity.ok(true);
    }
}

