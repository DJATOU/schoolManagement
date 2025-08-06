package com.school.management.service.payment;

import com.school.management.dto.PaymentDTO;
import com.school.management.dto.PaymentDetailDTO;
import com.school.management.persistance.PaymentEntity;
import com.school.management.persistance.PaymentDetailEntity;
import com.school.management.persistance.SessionEntity;
import com.school.management.repository.PaymentRepository;
import com.school.management.repository.PaymentDetailRepository;
import com.school.management.repository.AttendanceRepository;
import com.school.management.service.exception.CustomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service responsable de toutes les opérations de lecture et requêtes liées aux paiements.
 * Ne modifie pas les données, uniquement consultation.
 */
@Service
@Transactional(readOnly = true)
public class PaymentQueryService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentQueryService.class);

    private final PaymentRepository paymentRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final AttendanceRepository attendanceRepository;

    public PaymentQueryService(
            PaymentRepository paymentRepository,
            PaymentDetailRepository paymentDetailRepository,
            AttendanceRepository attendanceRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentDetailRepository = paymentDetailRepository;
        this.attendanceRepository = attendanceRepository;
    }

    /**
     * Récupère tous les paiements
     */
    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }

    /**
     * Récupère un paiement par son ID
     */
    public PaymentEntity getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new CustomServiceException("Payment not found with ID: " + id));
    }

    /**
     * Récupère tous les paiements d'un étudiant
     */
    public List<PaymentEntity> getAllPaymentsForStudent(Long studentId) {
        return paymentRepository.findAllByStudentIdOrderByPaymentDateDesc(studentId);
    }

    /**
     * Récupère un paiement existant pour une série donnée
     */
    public PaymentEntity findExistingPayment(Long studentId, Long groupId, Long sessionSeriesId) {
        return paymentRepository.findByStudentIdAndGroupIdAndSessionSeriesId(studentId, groupId, sessionSeriesId)
                .orElse(null);
    }

    /**
     * Récupère le montant total payé pour un étudiant et une série
     */
    public Double getTotalPaidForStudentAndSeries(Long studentId, Long sessionSeriesId) {
        Double totalPaid = paymentRepository.findAmountPaidForStudentAndSeries(studentId, sessionSeriesId);
        return totalPaid != null ? totalPaid : 0.0;
    }

    /**
     * Récupère les sessions auxquelles un étudiant a assisté
     */
    public List<SessionEntity> getAttendedSessions(Long studentId) {
        return attendanceRepository.findByStudentIdAndIsPresent(studentId, true);
    }

    /**
     * Récupère les sessions pour lesquelles un étudiant a payé
     */
    public Set<SessionEntity> getPaidSessions(Long studentId) {
        List<PaymentDetailEntity> details = paymentDetailRepository.findByPayment_StudentId(studentId);
        return details.stream()
                .map(PaymentDetailEntity::getSession)
                .collect(Collectors.toSet());
    }

    /**
     * Récupère les sessions auxquelles un étudiant a assisté mais n'a pas payées
     */
    public List<SessionEntity> getUnpaidAttendedSessions(Long studentId) {
        List<SessionEntity> attended = getAttendedSessions(studentId);
        Set<SessionEntity> paid = getPaidSessions(studentId);
        return attended.stream()
                .filter(session -> !paid.contains(session))
                .collect(Collectors.toList());
    }

    /**
     * Récupère les détails de paiement pour une série donnée
     */
    public List<PaymentDetailDTO> getPaymentDetailsForSeries(Long studentId, Long sessionSeriesId) {
        LOGGER.info("Fetching payment details for studentId={}, seriesId={}", studentId, sessionSeriesId);
        
        List<PaymentDetailEntity> details = paymentDetailRepository
                .findByPayment_StudentIdAndSession_SessionSeriesId(studentId, sessionSeriesId);
        
        LOGGER.debug("Payment details retrieved: {}", details);
        
        return details.stream()
                .map(this::convertToPaymentDetailDto)
                .collect(Collectors.toList());
    }

    /**
     * Récupère l'historique des paiements pour une série donnée
     */
    public List<PaymentEntity> getPaymentHistoryForSeries(Long studentId, Long sessionSeriesId) {
        LOGGER.info("Fetching payment history for studentId={}, seriesId={}", studentId, sessionSeriesId);
        
        List<PaymentEntity> payments = paymentRepository.findAllByStudentIdAndSessionSeriesId(studentId, sessionSeriesId);
        
        LOGGER.debug("Payment history retrieved: {}", payments);
        return payments;
    }

    /**
     * Vérifie le nombre de sessions auxquelles un étudiant a assisté pour une série
     */
    public long countAttendedSessionsForSeries(Long studentId, Long sessionSeriesId) {
        return attendanceRepository.countByStudentIdAndSessionSeriesIdAndIsPresent(studentId, sessionSeriesId, true);
    }

    /**
     * Récupère tous les détails de paiement pour un étudiant et une session spécifique
     */
    public List<PaymentDetailEntity> getPaymentDetailsForSession(Long studentId, Long sessionId) {
        return paymentDetailRepository.findByPayment_StudentIdAndSessionId(studentId, sessionId);
    }

    /**
     * Convertit une entité PaymentDetail en DTO
     */
    private PaymentDetailDTO convertToPaymentDetailDto(PaymentDetailEntity detail) {
        return PaymentDetailDTO.builder()
                .paymentDetailId(detail.getId())
                .sessionId(detail.getSession().getId())
                .sessionName(detail.getSession().getTitle())
                .amountPaid(detail.getAmountPaid())
                .remainingBalance(detail.getSession().getGroup().getPrice().getPrice() - detail.getAmountPaid())
                .build();
    }
}