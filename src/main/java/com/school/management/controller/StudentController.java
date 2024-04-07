package com.school.management.controller;

import com.school.management.dto.StudentDTO;
import com.school.management.mapper.StudentMapper;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.persistance.TutorEntity;
import com.school.management.service.PatchService;
import com.school.management.service.StudentService;
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
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private static final String STUDENT_NOT_FOUND_MESSAGE = "Student not found with id: ";
    private final StudentService studentService;

    @Value("${app.upload.dir}")
    private String uploadDir;
    private final StudentMapper studentMapper;

    @Autowired
    public StudentController(StudentService studentService, StudentMapper studentMapper, PatchService patchService) {
        this.studentService = studentService;
        this.studentMapper = studentMapper;
    }

    @PostMapping("/createStudent")
    public ResponseEntity<?> createStudent(@Valid @ModelAttribute StudentDTO studentDto,
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
            studentDto.setPhoto(filePath.toString());
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not save file: " + fileName);
        }

        try {
            StudentEntity student = studentMapper.studentDTOToStudent(studentDto);
            StudentEntity savedStudent = studentService.save(student);
            return ResponseEntity.ok(studentMapper.studentToStudentDTO(savedStudent));
        } catch (Exception e) {
            // Gestion des autres exceptions potentielles lors de la sauvegarde de l'étudiant
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Could not save student");
        }
    }



    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentDTO studentDto) {
        StudentEntity student = studentMapper.studentDTOToStudent(studentDto);
        student.setId(id);
        StudentEntity updatedStudent = studentService.save(student);
        return ResponseEntity.ok(studentMapper.studentToStudentDTO(updatedStudent));
    }

   @Transactional(readOnly = true)
   @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        List<StudentDTO> students;
        students = studentService.findAll().stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }
    @Transactional(readOnly = true)
    @GetMapping("/search")
    public ResponseEntity<List<StudentDTO>> searchStudents(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Long groupId,
            @RequestParam(required = false) String establishment) {

        List<StudentEntity> students = studentService.searchStudents(firstName, lastName, level, groupId, establishment);
        List<StudentDTO> studentDTOs = students.stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(studentDTOs);
    }


    @GetMapping("id/{id}")
    public ResponseEntity<StudentDTO> getStudentById(@PathVariable Long id) {
        StudentEntity student = studentService.findById(id)
                .orElseThrow(() -> new CustomServiceException(STUDENT_NOT_FOUND_MESSAGE + id));
        return ResponseEntity.ok(studentMapper.studentToStudentDTO(student));
    }

    @GetMapping("/groups/{groupId}")
    public ResponseEntity<List<StudentDTO>> getStudentsByGroupId(@PathVariable Long groupId) {
        List<StudentDTO> students = studentService.findByGroupsId(groupId).stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/levels/{level}")
    public ResponseEntity<List<StudentDTO>> getStudentsByLevel(@PathVariable String level) {
        List<StudentDTO> students = studentService.findByLevel(level).stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/establishments/{establishment}")
    public ResponseEntity<List<StudentDTO>> getStudentsByEstablishment(@PathVariable String establishment) {
        List<StudentDTO> students = studentService.findByEstablishment(establishment).stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }
    @Transactional(readOnly = true)
    @GetMapping("/firstnames/{firstName}/lastnames/{lastName}")
    public ResponseEntity<List<StudentDTO>> getStudentsByFirstNameAndLastName(@PathVariable String firstName, @PathVariable String lastName) {
        List<StudentDTO> students = studentService.findByFirstNameAndLastName(firstName, lastName).stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }

    @Transactional(readOnly = true)
    @GetMapping("/searchByNames")
    public ResponseEntity<List<StudentDTO>> getStudentsByFirstNameAndOrLastName(@RequestParam(required = false) String search) {
        List<StudentDTO> students = studentService.searchStudentsByNameStartingWithDTO(search); // Call the correct service method
        return ResponseEntity.ok(students);
    }
    @Transactional(readOnly = true)
    @GetMapping("/lastnames/{lastName}")
    public ResponseEntity<List<StudentDTO>> getStudentsByLastName(@PathVariable String lastName) {
        List<StudentDTO> students = studentService.findByLastName(lastName).stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}/tutor")
    public ResponseEntity<TutorEntity> getTutorForStudent(@PathVariable Long id) {
        StudentEntity student = studentService.findById(id)
                .orElseThrow(() -> new CustomServiceException(STUDENT_NOT_FOUND_MESSAGE + id));
        TutorEntity tutor = student.getTutor();
        return ResponseEntity.ok(tutor);
    }

    // get groups for student
    @GetMapping("/{id}/groups")
    public ResponseEntity<Set<GroupEntity>> getGroupsForStudent(@PathVariable Long id) {
        StudentEntity student = studentService.findById(id)
                .orElseThrow(() -> new CustomServiceException(STUDENT_NOT_FOUND_MESSAGE + id));
        Set<GroupEntity> groups = student.getGroups();
        return ResponseEntity.ok(groups);
    }



}