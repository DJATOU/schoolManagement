package com.school.management.controller;

import com.school.management.dto.StudentDTO;
import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.persistance.TutorEntity;
import com.school.management.repository.GroupRepository;
import com.school.management.repository.TutorRepository;
import com.school.management.service.StudentService;
import com.school.management.service.exception.CustomServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    private final StudentService studentService;

    private final TutorRepository tutorRepository;

    private final GroupRepository groupRepository;

    @Autowired
    public StudentController(StudentService studentService, TutorRepository tutorRepository, GroupRepository groupRepository) {
        this.studentService = studentService;
        this.tutorRepository = tutorRepository;
        this.groupRepository = groupRepository;
    }

    @PostMapping
    public ResponseEntity<StudentDTO> createStudent(@Valid @RequestBody StudentDTO studentDto) {
        StudentEntity student = convertToEntity(studentDto);
        StudentEntity savedStudent = studentService.save(student);
        return new ResponseEntity<>(convertToDto(savedStudent), HttpStatus.CREATED);
    }

    // Update student
    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentDTO studentDto) {
        StudentEntity student = convertToEntity(studentDto);
        student.setId(id);
        StudentEntity updatedStudent = studentService.save(student);
        return ResponseEntity.ok(convertToDto(updatedStudent));
    }

    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        List<StudentDTO> students = studentService.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    @GetMapping("/{id}/tutor")
    public ResponseEntity<TutorEntity> getTutorForStudent(@PathVariable Long id) {
        StudentEntity student = studentService.findById(id)
                .orElseThrow(() -> new CustomServiceException("Student not found with id: " + id));
        TutorEntity tutor = student.getTutor();
        return ResponseEntity.ok(tutor);
    }

    @GetMapping("/tutors/{tutorId}")
    public ResponseEntity<List<StudentDTO>> getStudentsForTutor(@PathVariable Long tutorId) {
        List<StudentDTO> students = studentService.findByTutorId(tutorId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    // get all student by group id
    @GetMapping("/groups/{groupId}")
    public ResponseEntity<List<StudentDTO>> getStudentsByGroupId(@PathVariable Long groupId) {
        List<StudentDTO> students = studentService.findByGroupsId(groupId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    // get all student by level
    @GetMapping("/levels/{level}")
    public ResponseEntity<List<StudentDTO>> getStudentsByLevel(@PathVariable String level) {
        List<StudentDTO> students = studentService.findByLevel(level).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    // get all student by establishment
    @GetMapping("/establishments/{establishment}")
    public ResponseEntity<List<StudentDTO>> getStudentsByEstablishment(@PathVariable String establishment) {
        List<StudentDTO> students = studentService.findByEstablishment(establishment).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    // get all student by first name and last name
    @GetMapping("/firstnames/{firstName}/lastnames/{lastName}")
    public ResponseEntity<List<StudentDTO>> getStudentsByFirstNameAndLastName(@PathVariable String firstName, @PathVariable String lastName) {
        List<StudentDTO> students = studentService.findByFirstNameAndLastName(firstName, lastName).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }

    // get all student by last name
    @GetMapping("/lastnames/{lastName}")
    public ResponseEntity<List<StudentDTO>> getStudentsByLastName(@PathVariable String lastName) {
        List<StudentDTO> students = studentService.findByLastName(lastName).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(students);
    }



    private StudentDTO convertToDto(StudentEntity student) {
        StudentDTO studentDto = new StudentDTO();
        studentDto.setFirstName(student.getFirstName());
        studentDto.setLastName(student.getLastName());
        studentDto.setEmail(student.getEmail());
        studentDto.setPhoneNumber(student.getPhoneNumber());
        studentDto.setDateOfBirth(student.getDateOfBirth());
        studentDto.setPlaceOfBirth(student.getPlaceOfBirth());
        studentDto.setPhoto(student.getPhoto());
        studentDto.setLevel(student.getLevel());
        studentDto.setEstablishment(student.getEstablishment());
        studentDto.setAverageScore(student.getAverageScore());

        if (student.getTutor() != null) {
            studentDto.setTutorId(student.getTutor().getId());
        }

        if (student.getGroups() != null) {
            studentDto.setGroupIds(student.getGroups().stream().map(GroupEntity::getId).collect(Collectors.toSet()));
        }

        return studentDto;
    }


    private StudentEntity convertToEntity(StudentDTO studentDto) {
        StudentEntity student = new StudentEntity();
        student.setFirstName(studentDto.getFirstName());
        student.setLastName(studentDto.getLastName());
        student.setEmail(studentDto.getEmail());
        student.setPhoneNumber(studentDto.getPhoneNumber());
        student.setDateOfBirth(studentDto.getDateOfBirth());
        student.setPlaceOfBirth(studentDto.getPlaceOfBirth());
        student.setPhoto(studentDto.getPhoto());
        student.setLevel(studentDto.getLevel());
        student.setEstablishment(studentDto.getEstablishment());
        student.setAverageScore(studentDto.getAverageScore());

        // For relationships like Tutor and Groups, we need to fetch them from the database
        // using their respective repository/service if the IDs are provided.
        if (studentDto.getTutorId() != null) {
            TutorEntity tutor = tutorRepository.findById(studentDto.getTutorId())
                    .orElseThrow(() -> new CustomServiceException("Tutor not found"));
            student.setTutor(tutor);
        }

        // Similar logic for groups, if they are provided
        if (studentDto.getGroupIds() != null && !studentDto.getGroupIds().isEmpty()) {
            Set<GroupEntity> groups = studentDto.getGroupIds().stream()
                    .map(groupId -> groupRepository.findById(groupId)
                            .orElseThrow(() -> new CustomServiceException("Group not found with id: " + groupId)))
                    .collect(Collectors.toSet());
            student.setGroups(groups);
        }

        return student;
    }


}


