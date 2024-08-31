package com.school.management.service;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.StudentEntity;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;

@Service
public class StudentPdfService {

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    public ByteArrayInputStream generateStudentProfilePdf(StudentEntity student, byte[] logoBytes, boolean isArabic) {
        try (PDDocument document = pdfGeneratorService.createDocument()) {
            PDPage page = pdfGeneratorService.addPage(document, PDRectangle.A4);

            // Charger la police arabe si nécessaire
            PDType0Font arabicFont = isArabic ? loadArabicFont(document) : null;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // Ajouter un cadre autour du contenu
                contentStream.setStrokingColor(0, 0, 0); // Couleur noire pour le cadre
                contentStream.setLineWidth(2);
                contentStream.addRect(40, 40, page.getMediaBox().getWidth() - 80, page.getMediaBox().getHeight() - 80);
                contentStream.stroke();

                // Ajouter le logo
                pdfGeneratorService.addImage(contentStream, document, logoBytes, 50, 720, 80, 80);

                // Ajouter le nom de l'étudiant
                contentStream.setFont(PDType1Font.HELVETICA_BOLD, 20);
                contentStream.beginText();
                contentStream.newLineAtOffset(150, 750);
                contentStream.showText(student.getFirstName() + " " + student.getLastName());
                contentStream.endText();

                // Ligne de séparation
                contentStream.setStrokingColor(100, 100, 100); // Gris clair pour la ligne
                contentStream.setLineWidth(1);
                contentStream.moveTo(40, 710);
                contentStream.lineTo(page.getMediaBox().getWidth() - 40, 710);
                contentStream.stroke();

                // Sections: Informations Personnelles et Académiques
                int xPositionLeft = 50;
                int yPosition = 680;

                if (isArabic && arabicFont != null) {
                    contentStream.setFont(arabicFont, 12);

                    // Ajouter les informations personnelles
                    pdfGeneratorService.addText(contentStream, reverseText("الاسم: " + student.getFirstName() + " " + student.getLastName()), xPositionLeft, yPosition, arabicFont, 12, true);
                    pdfGeneratorService.addText(contentStream, reverseText("الجنس: " + student.getGender()), xPositionLeft, yPosition -= 20, arabicFont, 12, true);
                    pdfGeneratorService.addText(contentStream, reverseText("البريد الإلكتروني: " + student.getEmail()), xPositionLeft, yPosition -= 20, arabicFont, 12, true);
                    pdfGeneratorService.addText(contentStream, reverseText("الهاتف: " + student.getPhoneNumber()), xPositionLeft, yPosition -= 20, arabicFont, 12, true);
                    pdfGeneratorService.addText(contentStream, reverseText("تاريخ الميلاد: " + new SimpleDateFormat("dd/MM/yyyy").format(student.getDateOfBirth())), xPositionLeft, yPosition -= 20, arabicFont, 12, true);
                    pdfGeneratorService.addText(contentStream, reverseText("مكان الميلاد: " + student.getPlaceOfBirth()), xPositionLeft, yPosition -= 20, arabicFont, 12, true);
                    pdfGeneratorService.addText(contentStream, reverseText("العنوان: " + student.getAddress()), xPositionLeft, yPosition -= 20, arabicFont, 12, true);

                    // Ajouter les informations académiques
                    pdfGeneratorService.addText(contentStream, reverseText("المؤسسة: " + student.getEstablishment()), xPositionLeft, yPosition -= 20, arabicFont, 12, true);
                    pdfGeneratorService.addText(contentStream, reverseText("المعدل: " + student.getAverageScore()), xPositionLeft, yPosition -= 20, arabicFont, 12, true);

                    // Ajouter les groupes en arabe
                    yPosition -= 40;  // Espacement supplémentaire
                    pdfGeneratorService.addText(contentStream, reverseText("المجموعات:"), xPositionLeft, yPosition, arabicFont, 14, true);
                    yPosition -= 20;
                    for (GroupEntity group : student.getGroups()) {
                        pdfGeneratorService.addText(contentStream, reverseText("- " + group.getName()), xPositionLeft + 20, yPosition, arabicFont, 12, true);
                        yPosition -= 20;
                    }
                } else {
                    // Ajouter les informations personnelles
                    pdfGeneratorService.addText(contentStream, "Informations Personnelles", xPositionLeft, yPosition, PDType1Font.HELVETICA_BOLD, 14);
                    pdfGeneratorService.addText(contentStream, "Genre: " + student.getGender(), xPositionLeft, yPosition -= 20, PDType1Font.HELVETICA, 12);
                    pdfGeneratorService.addText(contentStream, "Email: " + student.getEmail(), xPositionLeft, yPosition -= 20, PDType1Font.HELVETICA, 12);
                    pdfGeneratorService.addText(contentStream, "Téléphone: " + student.getPhoneNumber(), xPositionLeft, yPosition -= 20, PDType1Font.HELVETICA, 12);
                    pdfGeneratorService.addText(contentStream, "Date de naissance: " + new SimpleDateFormat("dd/MM/yyyy").format(student.getDateOfBirth()), xPositionLeft, yPosition -= 20, PDType1Font.HELVETICA, 12);
                    pdfGeneratorService.addText(contentStream, "Lieu de naissance: " + student.getPlaceOfBirth(), xPositionLeft, yPosition -= 20, PDType1Font.HELVETICA, 12);
                    pdfGeneratorService.addText(contentStream, "Adresse: " + student.getAddress(), xPositionLeft, yPosition -= 20, PDType1Font.HELVETICA, 12);

                    // Ajouter les informations académiques
                    int xPositionRight = 350;
                    yPosition = 680; // Réinitialiser la position Y pour la deuxième colonne
                    pdfGeneratorService.addText(contentStream, "Informations Académiques", xPositionRight, yPosition, PDType1Font.HELVETICA_BOLD, 14);
                    pdfGeneratorService.addText(contentStream, "Établissement: " + student.getEstablishment(), xPositionRight, yPosition -= 20, PDType1Font.HELVETICA, 12);
                    pdfGeneratorService.addText(contentStream, "Moyenne: " + student.getAverageScore(), xPositionRight, yPosition -= 20, PDType1Font.HELVETICA, 12);

                    // Ajouter les groupes en français
                    yPosition = 450; // Déplacez cette section plus bas
                    pdfGeneratorService.addText(contentStream, "Groupes:", xPositionLeft, yPosition, PDType1Font.HELVETICA_BOLD, 14);
                    yPosition -= 20;
                    for (GroupEntity group : student.getGroups()) {
                        pdfGeneratorService.addText(contentStream, "- " + group.getName(), xPositionLeft + 20, yPosition, PDType1Font.HELVETICA, 12);
                        yPosition -= 20;
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            document.close();
            return new ByteArrayInputStream(out.toByteArray());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }



    // Méthode pour inverser l'ordre des caractères d'une chaîne
    private String reverseText(String input) {
        return new StringBuilder(input).reverse().toString();
    }

    // Charger la police arabe
    public PDType0Font loadArabicFont(PDDocument document) throws IOException {
        ClassPathResource fontResource = new ClassPathResource("static/fonts/Amiri-Regular.ttf");
        return PDType0Font.load(document, fontResource.getInputStream());
    }
}
