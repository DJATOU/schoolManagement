package com.school.management.controller;

import com.school.management.persistance.StudentEntity;
import com.school.management.service.student.StudentPdfService;
import com.school.management.service.student.StudentService;
import com.school.management.service.exception.CustomServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/pdf")
public class PdfController {

    @Autowired
    private StudentPdfService studentPdfService;

    @Autowired
    private StudentService studentService;


    @GetMapping("/student/{id}")
    public ResponseEntity<byte[]> generateStudentPdf(@PathVariable Long id, @RequestParam(defaultValue = "fr") String lang) {
        StudentEntity student = studentService.findById(id)
                .orElseThrow(() -> new CustomServiceException("Student not found with id " + id));
        byte[] logoBytes = loadLogoBytes();

        // Déterminer la langue : 'ar' pour l'arabe, 'fr' par défaut pour le français
        boolean isArabic = "ar".equalsIgnoreCase(lang);

        ByteArrayInputStream bis = studentPdfService.generateStudentProfilePdf(student, logoBytes, isArabic);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "inline; filename=student-profile-" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(bis.readAllBytes());
    }

    private byte[] loadLogoBytes() {
        try {
            ClassPathResource imgFile = new ClassPathResource("static/images/succes_assistance.png");
            Path path = imgFile.getFile().toPath();
            return Files.readAllBytes(path);
        } catch (IOException e) {
            e.printStackTrace();
            throw new CustomServiceException("Failed to load logo image", e);
        }
    }
}
