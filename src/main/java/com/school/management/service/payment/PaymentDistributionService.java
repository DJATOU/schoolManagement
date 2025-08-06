package com.school.management.service.payment;

import com.school.management.persistance.PaymentEntity;
import com.school.management.persistance.PaymentDetailEntity;
import com.school.management.persistance.SessionEntity;
import com.school.management.repository.PaymentDetailRepository;
import com.school.management.repository.SessionRepository;
import com.school.management.service.exception.CustomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Service responsable de la distribution des paiements sur les sessions.
 * Gère la logique de répartition des montants payés sur les différentes sessions.
 */
@Service
@Transactional
public class PaymentDistributionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentDistributionService.class);

    private final PaymentDetailRepository paymentDetailRepository;
    private final SessionRepository sessionRepository;
    private final PaymentCalculationService calculationService;

    public PaymentDistributionService(
            PaymentDetailRepository paymentDetailRepository,
            SessionRepository sessionRepository,
            PaymentCalculationService calculationService
    ) {
        this.paymentDetailRepository = paymentDetailRepository;
        this.sessionRepository = sessionRepository;
        this.calculationService = calculationService;
    }

    /**
     * Distribue un paiement sur toutes les sessions d'une série par ordre chronologique
     */
    public void distributePaymentForSeries(PaymentEntity payment, Long sessionSeriesId, double amountToDistribute) {
        LOGGER.info("Starting payment distribution for payment {} on series {}, amount: {}", 
                    payment.getId(), sessionSeriesId, amountToDistribute);

        List<SessionEntity> sessions = getSessionsForSeriesSorted(sessionSeriesId);
        double pricePerSession = payment.getGroup().getPrice().getPrice();
        double remainingAmount = amountToDistribute;

        LOGGER.debug("Found {} sessions for distribution, price per session: {}", 
                     sessions.size(), pricePerSession);

        for (SessionEntity session : sessions) {
            if (remainingAmount <= 0) {
                LOGGER.debug("No remaining amount to distribute, stopping");
                break;
            }

            remainingAmount = distributePaymentForSession(payment, session, remainingAmount, pricePerSession);
        }

        // Vérification finale des surplus
        validateDistributionResult(payment, amountToDistribute, remainingAmount);

        LOGGER.info("Payment distribution completed. Remaining amount: {}", remainingAmount);
    }

    /**
     * Distribue un paiement sur une session spécifique
     */
    public double distributePaymentForSession(
            PaymentEntity payment, 
            SessionEntity session, 
            double remainingAmount, 
            double pricePerSession
    ) {
        LOGGER.debug("Distributing payment for session {}, remaining amount: {}", 
                     session.getId(), remainingAmount);

        Optional<PaymentDetailEntity> existingDetailOpt = paymentDetailRepository
                .findByPaymentIdAndSessionId(payment.getId(), session.getId());

        if (existingDetailOpt.isPresent()) {
            return updateExistingPaymentDetail(existingDetailOpt.get(), remainingAmount, pricePerSession);
        } else {
            return createNewPaymentDetail(payment, session, remainingAmount, pricePerSession);
        }
    }

    /**
     * Met à jour un détail de paiement existant
     */
    private double updateExistingPaymentDetail(
            PaymentDetailEntity existingDetail, 
            double remainingAmount, 
            double pricePerSession
    ) {
        double currentlyPaid = existingDetail.getAmountPaid();
        double amountNeeded = calculationService.calculateMinimumPaymentForSession(pricePerSession, currentlyPaid);

        if (amountNeeded > 0) {
            double amountToAdd = Math.min(amountNeeded, remainingAmount);
            double newTotalPaid = currentlyPaid + amountToAdd;
            
            existingDetail.setAmountPaid(newTotalPaid);
            paymentDetailRepository.save(existingDetail);
            
            LOGGER.debug("Updated existing payment detail: session {}, added {}, new total: {}", 
                         existingDetail.getSession().getId(), amountToAdd, newTotalPaid);
            
            return remainingAmount - amountToAdd;
        }

        LOGGER.debug("Session {} already fully paid, skipping", existingDetail.getSession().getId());
        return remainingAmount;
    }

    /**
     * Crée un nouveau détail de paiement
     */
    private double createNewPaymentDetail(
            PaymentEntity payment, 
            SessionEntity session, 
            double remainingAmount, 
            double pricePerSession
    ) {
        double amountToPay = Math.min(pricePerSession, remainingAmount);
        
        PaymentDetailEntity newDetail = new PaymentDetailEntity();
        newDetail.setPayment(payment);
        newDetail.setSession(session);
        newDetail.setAmountPaid(amountToPay);
        newDetail.setIsCatchUp(false); // Ce n'est pas un rattrapage
        
        paymentDetailRepository.save(newDetail);
        
        LOGGER.debug("Created new payment detail: session {}, amount: {}", 
                     session.getId(), amountToPay);
        
        return remainingAmount - amountToPay;
    }

    /**
     * Crée un paiement de rattrapage pour une session unique
     */
    public PaymentDetailEntity createCatchUpPayment(PaymentEntity payment, SessionEntity session, double amountPaid) {
        LOGGER.info("Creating catch-up payment for session {}, amount: {}", session.getId(), amountPaid);

        PaymentDetailEntity detail = new PaymentDetailEntity();
        detail.setPayment(payment);
        detail.setSession(session);
        detail.setAmountPaid(amountPaid);
        detail.setIsCatchUp(true); // Marquer comme rattrapage
        
        PaymentDetailEntity savedDetail = paymentDetailRepository.save(detail);
        
        LOGGER.debug("Catch-up payment detail created with ID: {}", savedDetail.getId());
        return savedDetail;
    }

    /**
     * Annule la distribution d'un paiement (pour les remboursements)
     */
    public void rollbackPaymentDistribution(PaymentEntity payment, double amountToRollback) {
        LOGGER.info("Rolling back payment distribution for payment {}, amount: {}", 
                    payment.getId(), amountToRollback);

        List<PaymentDetailEntity> details = paymentDetailRepository.findByPaymentId(payment.getId());
        details.sort((d1, d2) -> d2.getSession().getSessionTimeStart().compareTo(d1.getSession().getSessionTimeStart()));

        double remainingToRollback = amountToRollback;

        for (PaymentDetailEntity detail : details) {
            if (remainingToRollback <= 0) break;

            double currentPaid = detail.getAmountPaid();
            double amountToRemove = Math.min(currentPaid, remainingToRollback);
            
            if (amountToRemove >= currentPaid) {
                // Supprimer complètement ce détail
                paymentDetailRepository.delete(detail);
                LOGGER.debug("Deleted payment detail for session {}", detail.getSession().getId());
            } else {
                // Réduire le montant payé
                detail.setAmountPaid(currentPaid - amountToRemove);
                paymentDetailRepository.save(detail);
                LOGGER.debug("Reduced payment detail for session {}, new amount: {}", 
                             detail.getSession().getId(), detail.getAmountPaid());
            }
            
            remainingToRollback -= amountToRemove;
        }

        if (remainingToRollback > 0) {
            LOGGER.warn("Could not rollback full amount. Remaining: {}", remainingToRollback);
        }
    }

    /**
     * Récupère les sessions d'une série triées par ordre chronologique
     */
    private List<SessionEntity> getSessionsForSeriesSorted(Long sessionSeriesId) {
        List<SessionEntity> sessions = sessionRepository.findBySessionSeriesId(sessionSeriesId);
        sessions.sort(Comparator.comparing(SessionEntity::getSessionTimeStart));
        return sessions;
    }

    /**
     * Valide le résultat de la distribution
     */
    private void validateDistributionResult(PaymentEntity payment, double originalAmount, double remainingAmount) {
        if (remainingAmount > 0) {
            double totalCost = calculationService.calculateTotalSeriesCostForPayment(payment);
            double surplus = calculationService.calculateSurplus(payment.getAmountPaid(), totalCost);
            
            if (surplus > 0) {
                String message = String.format(
                    "Le paiement a été complété. Le montant excédentaire de %.2f euros sera remboursé.", 
                    surplus
                );
                LOGGER.info("Payment completed with surplus: {}", surplus);
                throw new CustomServiceException(message, HttpStatus.OK);
            }
        }
    }

    /**
     * Calcule le montant déjà distribué pour un paiement
     */
    public double calculateDistributedAmount(Long paymentId) {
        List<PaymentDetailEntity> details = paymentDetailRepository.findByPaymentId(paymentId);
        return details.stream()
                .mapToDouble(PaymentDetailEntity::getAmountPaid)
                .sum();
    }

    /**
     * Vérifie si une session est entièrement payée
     */
    public boolean isSessionFullyPaid(Long sessionId, double sessionCost) {
        List<PaymentDetailEntity> details = paymentDetailRepository.findBySessionId(sessionId);
        double totalPaid = details.stream()
                .mapToDouble(PaymentDetailEntity::getAmountPaid)
                .sum();
        
        return totalPaid >= sessionCost;
    }

    /**
     * Récupère tous les détails de paiement pour une session
     */
    public List<PaymentDetailEntity> getPaymentDetailsForSession(Long sessionId) {
        return paymentDetailRepository.findBySessionId(sessionId);
    }

    /**
     * Redistribue un paiement existant avec un nouveau montant
     */
    public void redistributePayment(PaymentEntity payment, double newAmount) {
        LOGGER.info("Redistributing payment {} with new amount: {}", payment.getId(), newAmount);

        // Annuler la distribution actuelle
        double currentDistributed = calculateDistributedAmount(payment.getId());
        rollbackPaymentDistribution(payment, currentDistributed);

        // Redistribuer avec le nouveau montant
        if (payment.getSessionSeries() != null) {
            distributePaymentForSeries(payment, payment.getSessionSeries().getId(), newAmount);
        }
    }
}