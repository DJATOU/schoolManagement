package com.school.management.controller;

import com.school.management.dto.TeacherDTO;
import com.school.management.mapper.TeacherMapper;
import com.school.management.persistance.TeacherEntity;
import com.school.management.service.TeacherService;
import com.school.management.service.exception.CustomServiceException;
import com.school.management.util.FileValidationUtil;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@RestController
@RequestMapping("/api/teachers")
public class TeacherController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TeacherController.class);

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
            Path uploadPath = Paths.get(uploadDir);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // IMPORTANT: Stocker UNIQUEMENT le nom du fichier (pas le chemin complet)
            // Cela unifie le comportement avec StudentController
            teacherDto.setPhoto(fileName);

            // Sauvegarder le professeur en base de données
            TeacherEntity teacher = teacherMapper.teacherDTOToTeacher(teacherDto);
            TeacherEntity savedTeacher = teacherService.save(teacher);
            LOGGER.info("Teacher created successfully with photo: {}", fileName);
            return ResponseEntity.ok(teacherMapper.teacherToTeacherDTO(savedTeacher));

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
            LOGGER.error("Could not save teacher", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Could not save teacher: " + e.getMessage());
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

