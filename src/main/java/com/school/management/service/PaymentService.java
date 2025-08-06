package com.school.management.service;

import com.school.management.dto.PaymentDTO;
import com.school.management.dto.PaymentDetailDTO;
import com.school.management.persistance.*;
import com.school.management.repository.*;
import com.school.management.service.exception.CustomServiceException;
import com.school.management.service.payment.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service principal de gestion des paiements.
 * Orchestre les différents services spécialisés pour traiter les paiements.
 * 
 * @author Refactorisé pour améliorer la maintenabilité
 */
@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);
    private static final String COMPLETED = "completed";
    private static final String IN_PROGRESS = "In Progress";

    // Repositories pour l'accès aux données
    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;
    private final SessionRepository sessionRepository;
    private final SessionSeriesRepository sessionSeriesRepository;

    // Services spécialisés
    private final PaymentQueryService paymentQueryService;
    private final PaymentCalculationService paymentCalculationService;
    private final PaymentValidationService paymentValidationService;
    private final PaymentDistributionService paymentDistributionService;
    private final PaymentStatusService paymentStatusService;

    public PaymentService(
            PaymentRepository paymentRepository,
            StudentRepository studentRepository,
            GroupRepository groupRepository,
            SessionRepository sessionRepository,
            SessionSeriesRepository sessionSeriesRepository,
            PaymentQueryService paymentQueryService,
            PaymentCalculationService paymentCalculationService,
            PaymentValidationService paymentValidationService,
            PaymentDistributionService paymentDistributionService,
            PaymentStatusService paymentStatusService
    ) {
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.sessionRepository = sessionRepository;
        this.sessionSeriesRepository = sessionSeriesRepository;
        this.paymentQueryService = paymentQueryService;
        this.paymentCalculationService = paymentCalculationService;
        this.paymentValidationService = paymentValidationService;
        this.paymentDistributionService = paymentDistributionService;
        this.paymentStatusService = paymentStatusService;
    }

    // ===========================================
    // CRUD Operations (délégation simple)
    // ===========================================

    public List<PaymentEntity> getAllPayments() {
        return paymentQueryService.getAllPayments();
    }

    public PaymentEntity getPaymentById(Long id) {
        return paymentQueryService.getPaymentById(id);
    }

    public PaymentEntity createPayment(PaymentEntity payment) {
        paymentValidationService.validatePaymentData(payment);
        return paymentRepository.save(payment);
    }

    public PaymentEntity updatePayment(Long id) {
        PaymentEntity existingPayment = getPaymentById(id);
        paymentValidationService.validatePaymentCanBeModified(existingPayment);
        return paymentRepository.save(existingPayment);
    }

    public List<PaymentEntity> getAllPaymentsForStudent(Long studentId) {
        return paymentQueryService.getAllPaymentsForStudent(studentId);
    }

    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    // ===========================================
    // Payment Processing (orchestration)
    // ===========================================

    /**
     * Traite un paiement pour une série complète
     */
    @Transactional
    public PaymentEntity processPayment(Long studentId, Long groupId, Long sessionSeriesId, double amountPaid) {
        LOGGER.info("Processing series payment: student={}, group={}, series={}, amount={}", 
                    studentId, groupId, sessionSeriesId, amountPaid);

        // 1. Récupération des entités
        StudentEntity student = getStudent(studentId);
        GroupEntity group = getGroup(groupId);
        SessionSeriesEntity series = getSessionSeries(sessionSeriesId);

        // 2. Récupération du paiement existant (si applicable)
        PaymentEntity existingPayment = paymentQueryService.findExistingPayment(studentId, groupId, sessionSeriesId);
        double currentTotalPaid = existingPayment != null ? existingPayment.getAmountPaid() : 0.0;

        // 3. Validation du paiement
        paymentValidationService.validateSeriesPayment(student, group, sessionSeriesId, amountPaid, currentTotalPaid);

        // 4. Création ou mise à jour du paiement
        PaymentEntity payment = getOrCreateSeriesPayment(student, group, series, amountPaid, existingPayment);

        // 5. Distribution du montant sur les sessions
        paymentDistributionService.distributePaymentForSeries(payment, sessionSeriesId, amountPaid);

        // 6. Sauvegarde finale
        PaymentEntity savedPayment = paymentRepository.save(payment);
        
        LOGGER.info("Series payment processed successfully: {}", savedPayment.getId());
        return savedPayment;
    }

    /**
     * Traite un paiement de rattrapage pour une session unique
     */
    @Transactional
    public PaymentEntity processCatchUpPayment(Long studentId, Long sessionId, double amountPaid) {
        LOGGER.info("Processing catch-up payment: student={}, session={}, amount={}", 
                    studentId, sessionId, amountPaid);

        // 1. Récupération des entités
        StudentEntity student = getStudent(studentId);
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomServiceException(
                        "Session not found with ID: " + sessionId, HttpStatus.BAD_REQUEST));

        // 2. Validation du paiement de rattrapage
        paymentValidationService.validateCatchUpPayment(student, session, amountPaid);

        // 3. Création du paiement pour la session unique
        PaymentEntity payment = createCatchUpPaymentEntity(student, session, amountPaid);

        // 4. Création du détail de paiement
        paymentDistributionService.createCatchUpPayment(payment, session, amountPaid);

        LOGGER.info("Catch-up payment processed successfully: {}", payment.getId());
        return payment;
    }

    // ===========================================
    // Payment Status & Analytics (délégation)
    // ===========================================

    public List<StudentPaymentStatus> getPaymentStatusForGroup(Long groupId) {
        return paymentStatusService.getPaymentStatusForGroup(groupId);
    }

    public boolean isStudentPaymentOverdueForSeries(Long studentId, Long sessionSeriesId, double pricePerSession) {
        return paymentStatusService.isStudentPaymentOverdueForSeries(studentId, sessionSeriesId, pricePerSession);
    }

    public List<GroupPaymentStatus> getPaymentStatusForStudent(Long studentId) {
        return paymentStatusService.getPaymentStatusForStudent(studentId);
    }

    // ===========================================
    // Data Retrieval (délégation)
    // ===========================================

    public List<SessionEntity> getAttendedSessions(Long studentId) {
        return paymentQueryService.getAttendedSessions(studentId);
    }

    public List<SessionEntity> getUnpaidAttendedSessions(Long studentId) {
        return paymentQueryService.getUnpaidAttendedSessions(studentId);
    }

    public List<PaymentDetailDTO> getPaymentDetailsForSeries(Long studentId, Long sessionSeriesId) {
        return paymentQueryService.getPaymentDetailsForSeries(studentId, sessionSeriesId);
    }

    public List<PaymentDTO> getPaymentHistoryForSeries(Long studentId, Long sessionSeriesId) {
        List<PaymentEntity> payments = paymentQueryService.getPaymentHistoryForSeries(studentId, sessionSeriesId);
        return payments.stream()
                .map(this::convertToDto)
                .toList();
    }

    // ===========================================
    // Private Helper Methods
    // ===========================================

    private StudentEntity getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new CustomServiceException("Student not found with ID: " + studentId));
    }

    private GroupEntity getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new CustomServiceException("Group not found with ID: " + groupId));
    }

    private SessionSeriesEntity getSessionSeries(Long seriesId) {
        return sessionSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new CustomServiceException("Series not found with ID: " + seriesId));
    }

    /**
     * Crée ou met à jour un paiement pour une série
     */
    private PaymentEntity getOrCreateSeriesPayment(
            StudentEntity student,
            GroupEntity group,
            SessionSeriesEntity series,
            double amountPaid,
            PaymentEntity existingPayment
    ) {
        double totalCost = paymentCalculationService.calculateTotalSeriesCost(group);

        if (existingPayment != null) {
            // Mise à jour du paiement existant
            double newTotal = existingPayment.getAmountPaid() + amountPaid;
            existingPayment.setAmountPaid(newTotal);
            existingPayment.setStatus(newTotal >= totalCost ? COMPLETED : IN_PROGRESS);
            return paymentRepository.save(existingPayment);
        } else {
            // Création d'un nouveau paiement
            PaymentEntity payment = new PaymentEntity();
            payment.setStudent(student);
            payment.setGroup(group);
            payment.setSessionSeries(series);
            payment.setAmountPaid(amountPaid);
            payment.setStatus(amountPaid >= totalCost ? COMPLETED : IN_PROGRESS);
            return paymentRepository.save(payment);
        }
    }

    /**
     * Crée un paiement pour un rattrapage
     */
    private PaymentEntity createCatchUpPaymentEntity(StudentEntity student, SessionEntity session, double amountPaid) {
        double sessionCost = paymentCalculationService.calculateSessionCost(session.getGroup());

        PaymentEntity payment = new PaymentEntity();
        payment.setStudent(student);
        payment.setGroup(session.getGroup());
        payment.setSession(session);
        payment.setAmountPaid(amountPaid);
        payment.setStatus(amountPaid >= sessionCost ? COMPLETED : IN_PROGRESS);
        
        return paymentRepository.save(payment);
    }

    /**
     * Convertit une entité Payment en DTO
     */
    public PaymentDTO convertToDto(PaymentEntity payment) {
        return PaymentDTO.builder()
                .studentId(payment.getStudent().getId())
                .groupId(payment.getGroup() != null ? payment.getGroup().getId() : null)
                .sessionSeriesId(payment.getSessionSeries() != null ? payment.getSessionSeries().getId() : null)
                .sessionId(payment.getSession() != null ? payment.getSession().getId() : null)
                .amountPaid(payment.getAmountPaid())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDescription(payment.getDescription())
                .totalSeriesCost(paymentCalculationService.calculateTotalSeriesCostForPayment(payment))
                .totalPaidForSeries(paymentCalculationService.calculateTotalPaidForSeries(payment))
                .amountOwed(paymentCalculationService.calculateAmountOwed(payment))
                .build();
    }

    // ===========================================
    // Méthodes de convenance pour la compatibilité
    // ===========================================

    /**
     * @deprecated Utiliser les services spécialisés directement
     */
    @Deprecated
    private double calculateTotalCost(GroupEntity group) {
        return paymentCalculationService.calculateTotalSeriesCost(group);
    }

    /**
     * @deprecated Utiliser PaymentDistributionService.distributePaymentForSeries
     */
    @Deprecated
    public void distributePayment(PaymentEntity payment, Long sessionSeriesId, double amountPaid) {
        paymentDistributionService.distributePaymentForSeries(payment, sessionSeriesId, amountPaid);
    }
}
