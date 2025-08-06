package com.school.management.service.payment;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.PaymentEntity;
import com.school.management.persistance.SessionEntity;
import com.school.management.persistance.StudentEntity;
import com.school.management.service.exception.CustomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * Service responsable de la validation des règles métier pour les paiements.
 * Centralise toute la logique de validation avant traitement des paiements.
 */
@Service
public class PaymentValidationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentValidationService.class);

    private final PaymentCalculationService calculationService;

    public PaymentValidationService(PaymentCalculationService calculationService) {
        this.calculationService = calculationService;
    }

    /**
     * Valide qu'un paiement de série peut être effectué
     */
    public void validateSeriesPayment(
            StudentEntity student, 
            GroupEntity group, 
            Long sessionSeriesId, 
            double amountPaid, 
            double currentTotalPaid
    ) {
        LOGGER.debug("Validating series payment for student {}, group {}, amount {}", 
                     student.getId(), group.getId(), amountPaid);

        // Validation des paramètres de base
        validateBasicPaymentParameters(student, group, amountPaid);

        double newTotalAmount = currentTotalPaid + amountPaid;
        double totalSeriesCost = calculationService.calculateTotalSeriesCost(group);

        // Validation du dépassement du coût total
        validatePaymentNotExceedingTotalCost(newTotalAmount, totalSeriesCost);

        // Validation par rapport aux sessions créées
        validatePaymentAgainstCreatedSessions(sessionSeriesId, newTotalAmount, group);
    }

    /**
     * Valide qu'un paiement de rattrapage peut être effectué
     */
    public void validateCatchUpPayment(
            StudentEntity student, 
            SessionEntity session, 
            double amountPaid
    ) {
        LOGGER.debug("Validating catch-up payment for student {}, session {}, amount {}", 
                     student.getId(), session.getId(), amountPaid);

        // Validation des paramètres de base
        validateBasicPaymentParameters(student, session.getGroup(), amountPaid);

        double sessionCost = calculationService.calculateSessionCost(session.getGroup());

        // Validation du dépassement du coût de la session
        if (amountPaid > sessionCost) {
            double surplus = amountPaid - sessionCost;
            String message = String.format(
                "Le montant payé dépasse le coût de la session de %.2f euros.", surplus
            );
            LOGGER.warn("Catch-up payment validation failed: {}", message);
            throw new CustomServiceException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide les paramètres de base d'un paiement
     */
    public void validateBasicPaymentParameters(StudentEntity student, GroupEntity group, double amountPaid) {
        if (student == null) {
            throw new CustomServiceException("L'étudiant ne peut pas être null", HttpStatus.BAD_REQUEST);
        }

        if (group == null) {
            throw new CustomServiceException("Le groupe ne peut pas être null", HttpStatus.BAD_REQUEST);
        }

        if (group.getPrice() == null) {
            throw new CustomServiceException("Le prix du groupe n'est pas défini", HttpStatus.BAD_REQUEST);
        }

        if (amountPaid <= 0) {
            throw new CustomServiceException("Le montant du paiement doit être positif", HttpStatus.BAD_REQUEST);
        }

        if (!student.getActive()) {
            throw new CustomServiceException("Impossible d'effectuer un paiement pour un étudiant inactif", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide qu'un paiement ne dépasse pas le coût total d'une série
     */
    public void validatePaymentNotExceedingTotalCost(double newTotalAmount, double totalSeriesCost) {
        if (newTotalAmount > totalSeriesCost) {
            double surplus = newTotalAmount - totalSeriesCost;
            String message = String.format(
                "Le montant payé dépasse le coût total de la série de %.2f euros.", surplus
            );
            LOGGER.warn("Payment validation failed: {}", message);
            throw new CustomServiceException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide qu'un paiement ne dépasse pas le coût des sessions créées
     */
    public void validatePaymentAgainstCreatedSessions(Long seriesId, double newTotalAmount, GroupEntity group) {
        if (!calculationService.canProcessPayment(seriesId, newTotalAmount, group)) {
            String message = "Le paiement ne peut pas être effectué car il dépasse le coût des sessions créées.";
            LOGGER.warn("Payment validation failed: {}", message);
            throw new CustomServiceException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide qu'un étudiant est inscrit dans un groupe
     */
    public void validateStudentInGroup(StudentEntity student, GroupEntity group) {
        boolean isStudentInGroup = student.getGroups().stream()
                .anyMatch(g -> g.getId().equals(group.getId()));

        if (!isStudentInGroup) {
            String message = String.format(
                "L'étudiant %s n'est pas inscrit dans le groupe %s", 
                student.getFirstName() + " " + student.getLastName(),
                group.getName()
            );
            LOGGER.warn("Student group validation failed: {}", message);
            throw new CustomServiceException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide qu'une session appartient bien à une série donnée
     */
    public void validateSessionBelongsToSeries(SessionEntity session, Long sessionSeriesId) {
        if (!session.getSessionSeries().getId().equals(sessionSeriesId)) {
            String message = String.format(
                "La session %d n'appartient pas à la série %d", 
                session.getId(), 
                sessionSeriesId
            );
            LOGGER.warn("Session series validation failed: {}", message);
            throw new CustomServiceException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide qu'un montant de paiement partiel est acceptable
     */
    public void validatePartialPayment(double amountPaid, double sessionCost, double minimumPercentage) {
        double paymentPercentage = calculationService.calculatePaymentPercentage(amountPaid, sessionCost);
        
        if (paymentPercentage < minimumPercentage) {
            String message = String.format(
                "Le paiement partiel de %.2f%% est inférieur au minimum requis de %.2f%%", 
                paymentPercentage, 
                minimumPercentage
            );
            LOGGER.warn("Partial payment validation failed: {}", message);
            throw new CustomServiceException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide qu'un paiement peut être modifié (n'est pas déjà complété)
     */
    public void validatePaymentCanBeModified(PaymentEntity payment) {
        if (payment == null) {
            throw new CustomServiceException("Le paiement n'existe pas", HttpStatus.NOT_FOUND);
        }

        if ("completed".equalsIgnoreCase(payment.getStatus())) {
            String message = "Impossible de modifier un paiement déjà complété";
            LOGGER.warn("Payment modification validation failed: {}", message);
            throw new CustomServiceException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide les règles de remboursement
     */
    public void validateRefund(PaymentEntity payment, double refundAmount) {
        validatePaymentCanBeModified(payment);

        if (refundAmount <= 0) {
            throw new CustomServiceException("Le montant du remboursement doit être positif", HttpStatus.BAD_REQUEST);
        }

        if (refundAmount > payment.getAmountPaid()) {
            String message = String.format(
                "Le montant du remboursement (%.2f€) ne peut pas dépasser le montant payé (%.2f€)", 
                refundAmount, 
                payment.getAmountPaid()
            );
            LOGGER.warn("Refund validation failed: {}", message);
            throw new CustomServiceException(message, HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * Valide les données d'un paiement avant sauvegarde
     */
    public void validatePaymentData(PaymentEntity payment) {
        if (payment.getStudent() == null) {
            throw new CustomServiceException("L'étudiant doit être spécifié", HttpStatus.BAD_REQUEST);
        }

        if (payment.getGroup() == null) {
            throw new CustomServiceException("Le groupe doit être spécifié", HttpStatus.BAD_REQUEST);
        }

        if (payment.getAmountPaid() == null || payment.getAmountPaid() < 0) {
            throw new CustomServiceException("Le montant payé doit être positif", HttpStatus.BAD_REQUEST);
        }

        if (payment.getStatus() == null || payment.getStatus().trim().isEmpty()) {
            throw new CustomServiceException("Le statut du paiement doit être spécifié", HttpStatus.BAD_REQUEST);
        }
    }
}