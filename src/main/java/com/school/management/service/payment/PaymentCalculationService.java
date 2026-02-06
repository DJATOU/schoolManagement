package com.school.management.service.payment;

import com.school.management.persistance.GroupEntity;
import com.school.management.persistance.PaymentEntity;
import com.school.management.persistance.SessionSeriesEntity;
import com.school.management.repository.SessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Service responsable de tous les calculs liés aux paiements.
 * Centralise la logique de calcul des coûts, montants dus, etc.
 */
@Service
public class PaymentCalculationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentCalculationService.class);

    private final SessionRepository sessionRepository;

    public PaymentCalculationService(SessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Calcule le coût total d'une série basé sur le prix par session et le nombre de sessions prévues
     */
    public double calculateTotalSeriesCost(GroupEntity group) {
        if (group == null || group.getPrice() == null) {
            LOGGER.warn("Group or price is null, returning 0.0");
            return 0.0;
        }
        
        double pricePerSession = group.getPrice().getPrice();
        int sessionNumberPerSerie = group.getSessionNumberPerSerie();
        double totalCost = pricePerSession * sessionNumberPerSerie;
        
        LOGGER.debug("Calculated total series cost: {} (price per session: {}, sessions: {})", 
                     totalCost, pricePerSession, sessionNumberPerSerie);
        
        return totalCost;
    }

    /**
     * Calcule le coût des sessions effectivement créées pour une série
     */
    public double calculateCreatedSessionsCost(Long seriesId, GroupEntity group) {
        if (group == null || group.getPrice() == null) {
            LOGGER.warn("Group or price is null, returning 0.0");
            return 0.0;
        }
        
        int createdSessionsCount = sessionRepository.countBySessionSeriesId(seriesId);
        double pricePerSession = group.getPrice().getPrice();
        double createdCost = createdSessionsCount * pricePerSession;
        
        LOGGER.debug("Calculated created sessions cost: {} (created sessions: {}, price per session: {})", 
                     createdCost, createdSessionsCount, pricePerSession);
        
        return createdCost;
    }

    /**
     * Calcule le coût d'une session unique
     */
    public double calculateSessionCost(GroupEntity group) {
        if (group == null || group.getPrice() == null) {
            LOGGER.warn("Group or price is null, returning 0.0");
            return 0.0;
        }
        
        return group.getPrice().getPrice();
    }

    /**
     * Calcule le montant total dû basé sur le nombre de sessions auxquelles l'étudiant a assisté
     */
    public double calculateAmountDueForAttendedSessions(long attendedSessionsCount, double pricePerSession) {
        double amountDue = attendedSessionsCount * pricePerSession;
        
        LOGGER.debug("Calculated amount due for attended sessions: {} (sessions: {}, price: {})", 
                     amountDue, attendedSessionsCount, pricePerSession);
        
        return amountDue;
    }

    /**
     * Calcule le montant restant à payer pour une série
     */
    public double calculateRemainingAmountForSeries(PaymentEntity payment) {
        if (payment == null || payment.getGroup() == null) {
            return 0.0;
        }
        
        double totalCost = calculateTotalSeriesCostForPayment(payment);
        double amountPaid = payment.getAmountPaid();
        double remaining = Math.max(0, totalCost - amountPaid);
        
        LOGGER.debug("Calculated remaining amount: {} (total cost: {}, paid: {})", 
                     remaining, totalCost, amountPaid);
        
        return remaining;
    }

    /**
     * Calcule le surplus de paiement (montant payé en excès)
     */
    public double calculateSurplus(double amountPaid, double totalCost) {
        double surplus = Math.max(0, amountPaid - totalCost);
        
        if (surplus > 0) {
            LOGGER.debug("Calculated surplus: {} (paid: {}, cost: {})", surplus, amountPaid, totalCost);
        }
        
        return surplus;
    }

    /**
     * Calcule le coût total d'une série pour un paiement donné
     */
    public double calculateTotalSeriesCostForPayment(PaymentEntity payment) {
        if (payment.getGroup() == null || payment.getSessionSeries() == null) {
            return 0.0;
        }
        
        double pricePerSession = payment.getGroup().getPrice().getPrice();
        int sessionCount = payment.getSessionSeries().getSessions().size();
        
        return pricePerSession * sessionCount;
    }

    /**
     * Calcule le montant total payé pour une série (utile pour les DTOs)
     */
    public double calculateTotalPaidForSeries(PaymentEntity payment) {
        return payment != null ? payment.getAmountPaid() : 0.0;
    }

    /**
     * Calcule le montant restant dû pour une série (pour les DTOs)
     */
    public double calculateAmountOwed(PaymentEntity payment) {
        double totalCost = calculateTotalSeriesCostForPayment(payment);
        double totalPaid = calculateTotalPaidForSeries(payment);
        
        return Math.max(0, totalCost - totalPaid);
    }

    /**
     * Calcule le montant minimum nécessaire pour compléter le paiement d'une session
     */
    public double calculateMinimumPaymentForSession(double sessionCost, double alreadyPaid) {
        return Math.max(0, sessionCost - alreadyPaid);
    }

    /**
     * Calcule le pourcentage de paiement effectué pour une série
     */
    public double calculatePaymentPercentage(double amountPaid, double totalCost) {
        if (totalCost <= 0) {
            return 0.0;
        }
        
        double percentage = (amountPaid / totalCost) * 100;
        return Math.min(100.0, percentage);
    }

    /**
     * Vérifie si un paiement peut être effectué sans dépasser le coût des sessions créées
     */
    public boolean canProcessPayment(Long seriesId, double newTotalAmount, GroupEntity group) {
        double totalCreatedCost = calculateCreatedSessionsCost(seriesId, group);
        boolean canProcess = newTotalAmount <= totalCreatedCost;
        
        LOGGER.debug("Can process payment: {} (new total: {}, created cost: {})", 
                     canProcess, newTotalAmount, totalCreatedCost);
        
        return canProcess;
    }

    /**
     * Calcule le nombre de sessions qui peuvent être payées avec un montant donné
     */
    public int calculateSessionsPayableWithAmount(double amount, double pricePerSession) {
        if (pricePerSession <= 0) {
            return 0;
        }
        
        return (int) Math.floor(amount / pricePerSession);
    }
}