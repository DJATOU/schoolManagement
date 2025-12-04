package com.school.management.controller;

import com.school.management.dto.GroupDTO;
import com.school.management.dto.StudentDTO;
import com.school.management.dto.student.StudentFullHistoryDTO;
import com.school.management.mapper.GroupMapper;
import com.school.management.mapper.StudentMapper;
import com.school.management.persistance.StudentEntity;
import com.school.management.persistance.TutorEntity;
import com.school.management.service.exception.CustomServiceException;
import com.school.management.service.student.StudentHistoryService;
import com.school.management.service.student.StudentService;
import com.school.management.util.FileValidationUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private static final Logger LOGGER = LoggerFactory.getLogger(StudentController.class);
    private static final String STUDENT_NOT_FOUND_MESSAGE = "Student not found with id: ";
    private final StudentService studentService;

    @Value("${app.upload.dir}")
    private String uploadDir;
    private final StudentMapper studentMapper;
    private final GroupMapper groupMapper;

    private final StudentHistoryService studentHistoryService;

    @Autowired
    public StudentController(StudentService studentService, StudentMapper studentMapper, GroupMapper groupMapper, StudentHistoryService studentHistoryService) {
        this.studentService = studentService;
        this.studentMapper = studentMapper;
        this.groupMapper = groupMapper;
        this.studentHistoryService = studentHistoryService;
    }


    @PostMapping("/createStudent")
    public ResponseEntity<Object> createStudent(@Valid @ModelAttribute StudentDTO studentDto,
                                           @RequestParam("file") MultipartFile file) {
        // Validation complète du fichier (type, taille, sécurité)
        try {
            FileValidationUtil.validateImageFile(file);
        } catch (IllegalArgumentException e) {
            LOGGER.warn("File validation failed: {}", e.getMessage());
            return ResponseEntity.badRequest().body(e.getMessage());
        }

        // Générer un nom unique et sécurisé pour l'image
        String fileName = FileValidationUtil.generateSafeFilename(file.getOriginalFilename());
        Path filePath = null;

        try {
            // Sauvegarder l'image sur le disque
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // Stocker uniquement le nom de l'image dans la base de données
            studentDto.setPhoto(fileName);

            // Sauvegarder l'étudiant en base de données
            StudentEntity student = studentMapper.studentDTOToStudent(studentDto);
            StudentEntity savedStudent = studentService.save(student);
            LOGGER.info("Student created successfully with photo: {}", fileName);
            return ResponseEntity.ok(studentMapper.studentToStudentDTO(savedStudent));

        } catch (IOException e) {
            LOGGER.error("Could not save file: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not save file: " + fileName);
        } catch (Exception e) {
            // Nettoyer le fichier si la sauvegarde en base a échoué
            if (filePath != null && Files.exists(filePath)) {
                try {
                    Files.delete(filePath);
                    LOGGER.info("Deleted orphan file after DB save failure: {}", fileName);
                } catch (IOException deleteEx) {
                    LOGGER.error("Failed to delete orphan file: {}", fileName, deleteEx);
                }
            }
            LOGGER.error("Could not save student", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not save student: " + e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<StudentDTO> updateStudent(@PathVariable Long id, @Valid @RequestBody StudentDTO studentDto) {
        // Récupérer l'étudiant existant depuis la base de données
        StudentEntity existingStudent = studentService.findById(id)
                .orElseThrow(() -> new CustomServiceException("Student not found with id: " + id));

        // Mettre à jour l'entité existante avec les valeurs du DTO

        // Sauvegarder l'entité mise à jour
        StudentEntity updatedStudent = studentService.save(existingStudent);

        // Retourner le DTO mis à jour
        return ResponseEntity.ok(studentMapper.studentToStudentDTO(updatedStudent));
    }


    @Transactional(readOnly = true)
    @GetMapping
    public ResponseEntity<List<StudentDTO>> getAllStudents() {
        List<StudentDTO> students = studentService.findAllActiveStudents().stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }


    @Transactional(readOnly = true)
    @GetMapping("/search")
    public ResponseEntity<List<StudentDTO>> searchStudents(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Long level,
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
        StudentDTO studentDto = studentMapper.studentToStudentDTO(student);

        return ResponseEntity.ok(studentDto);
    }


    @GetMapping("/groups/{groupId}")
    public ResponseEntity<List<StudentDTO>> getStudentsByGroupId(@PathVariable Long groupId) {
        List<StudentDTO> students = studentService.findByGroupsId(groupId).stream()
                .map(studentMapper::studentToStudentDTO)
                .toList();
        return ResponseEntity.ok(students);
    }

    @GetMapping("/levels/{level}")
    public ResponseEntity<List<StudentDTO>> getStudentsByLevel(@PathVariable long level) {
        List<StudentDTO> students = studentService.findByLevel(level).stream()
                .filter(student -> Boolean.TRUE.equals(student.getActive())) // Filtrer les étudiants actifs
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
        List<StudentDTO> students = studentService.searchStudentsByNameStartingWithDTO(search);
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
    public ResponseEntity<Set<GroupDTO>> getGroupsForStudent(@PathVariable Long id) {
        StudentEntity student = studentService.findById(id)
                .orElseThrow(() -> new CustomServiceException("Student not found with id " + id));
        Set<GroupDTO> groupDTOs = student.getGroups().stream()
                .map(groupMapper::groupToGroupDTO)
                .collect(Collectors.toSet());
        return ResponseEntity.ok(groupDTOs);
    }

    // desactivate user
    @DeleteMapping("/disable/{id}")
    public ResponseEntity<Boolean> desactivateStudent(@PathVariable Long id) {
        studentService.desactivateStudent(id);
        return ResponseEntity.ok(true);
    }

    @GetMapping("/photos/{fileName}")
    public ResponseEntity<Resource> getPhoto(@PathVariable String fileName) {
        // Validation Path Traversal - Sécurité critique
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Path filePath = uploadPath.resolve(fileName).normalize();

            // Vérifier que le fichier résolu est bien dans le répertoire autorisé
            if (!filePath.startsWith(uploadPath)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (resource.exists() && resource.isReadable()) {
                MediaType mediaType = getMediaTypeForFileName(fileName);
                return ResponseEntity.ok()
                        .contentType(mediaType)
                        .body(resource);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    private MediaType getMediaTypeForFileName(String filename) {
        String extension = getFileExtension(filename).toLowerCase();
        return switch (extension) {
            case "png" -> MediaType.IMAGE_PNG;
            case "gif" -> MediaType.IMAGE_GIF;
            case "webp" -> MediaType.valueOf("image/webp");
            case "svg" -> MediaType.valueOf("image/svg+xml");
            default -> MediaType.IMAGE_JPEG;
        };
    }

    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "";
    }



    @GetMapping("/{studentId}/full-history")
    public ResponseEntity<StudentFullHistoryDTO> getStudentFullHistory(@PathVariable Long studentId) {
        StudentFullHistoryDTO fullHistory = studentHistoryService.getStudentFullHistory(studentId);
        return ResponseEntity.ok(fullHistory);
    }


}