package com.school.management.service;

import com.school.management.dto.PaymentDTO;
import com.school.management.dto.PaymentDetailDTO;
import com.school.management.persistance.*;
import com.school.management.repository.*;
import com.school.management.service.exception.CustomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * PaymentService handles all payment-related operations,
 * including full-series payments and single-session (catch-up) payments.
 */
@Service
public class PaymentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentService.class);
    private static final String COMPLETED = "completed";

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final SessionSeriesRepository sessionSeriesRepository;

    public PaymentService(
            PaymentRepository paymentRepository,
            StudentRepository studentRepository,
            GroupRepository groupRepository,
            PaymentDetailRepository paymentDetailRepository,
            SessionRepository sessionRepository,
            AttendanceRepository attendanceRepository,
            SessionSeriesRepository sessionSeriesRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.paymentDetailRepository = paymentDetailRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionSeriesRepository = sessionSeriesRepository;
    }

    // --------------------------
    // Basic Payment CRUD methods
    // --------------------------

    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }

    public PaymentEntity getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found with ID: " + id));
    }

    public PaymentEntity createPayment(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    /**
     * Example update method if you want to do a full update on existing Payment.
     */
    public PaymentEntity updatePayment(Long id) {
        PaymentEntity existingPayment = getPaymentById(id);
        // Update relevant fields from an input (not shown)...
        return paymentRepository.save(existingPayment);
    }

    public List<PaymentEntity> getAllPaymentsForStudent(Long studentId) {
        return paymentRepository.findAllByStudentIdOrderByPaymentDateDesc(studentId);
    }

    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    // -------------------------------------------
    // Full-series Payment (distributing amount)
    // -------------------------------------------

    /**
     * processPayment - For paying an entire series.
     * Distributes the 'amountPaid' across all sessions in the series.
     */
    @Transactional
    public PaymentEntity processPayment(Long studentId, Long groupId, Long sessionSeriesId, double amountPaid) {
        StudentEntity student = getStudent(studentId);
        GroupEntity group = getGroup(groupId);
        SessionSeriesEntity series = getSessionSeries(sessionSeriesId);

        double totalSeriesCost = calculateTotalCost(group);
        Optional<PaymentEntity> existingPaymentOpt = paymentRepository
                .findByStudentIdAndGroupIdAndSessionSeriesId(studentId, groupId, sessionSeriesId);

        double currentTotalPaid = existingPaymentOpt.map(PaymentEntity::getAmountPaid).orElse(0.0);
        double newTotalAmount = currentTotalPaid + amountPaid;

        // 1) Check if new total surpasses total cost
        if (newTotalAmount > totalSeriesCost) {
            double surplus = newTotalAmount - totalSeriesCost;
            throw new CustomServiceException(
                    "Le montant payé dépasse le coût total de la série de " + surplus + " euros.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 2) Check if payment surpasses created sessions cost
        if (!canProcessPayment(sessionSeriesId, newTotalAmount, group)) {
            throw new CustomServiceException(
                    "Le paiement ne peut pas être effectué car il dépasse le coût des sessions créées.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // 3) Create or update Payment, then distribute
        PaymentEntity payment = getOrCreateSeriesPayment(student, group, series, amountPaid);
        distributePayment(payment, sessionSeriesId, amountPaid);

        // 4) Save final status
        return paymentRepository.save(payment);
    }

    /**
     * getOrCreateSeriesPayment either updates existing Payment or creates a new Payment
     * for the entire series. It does not do distribution logic (that is in distributePayment).
     */
    private PaymentEntity getOrCreateSeriesPayment(
            StudentEntity student,
            GroupEntity group,
            SessionSeriesEntity series,
            double amountPaid
    ) {
        double totalCost = calculateTotalCost(group);
        var existingOpt = paymentRepository.findByStudentIdAndGroupIdAndSessionSeriesId(
                student.getId(), group.getId(), series.getId()
        );

        PaymentEntity payment;
        if (existingOpt.isPresent()) {
            payment = existingOpt.get();
            double newTotal = payment.getAmountPaid() + amountPaid;

            double createdSessionsCost = calculateCreatedSessionsCost(series.getId(), group);
            if (newTotal > createdSessionsCost) {
                throw new CustomServiceException(
                        "Le paiement total dépasse le coût des sessions créées.",
                        HttpStatus.BAD_REQUEST
                );
            }

            payment.setAmountPaid(newTotal);
            payment.setStatus(newTotal >= totalCost ? COMPLETED : "In Progress");
        } else {
            payment = new PaymentEntity();
            payment.setStudent(student);
            payment.setGroup(group);
            payment.setSessionSeries(series);
            payment.setAmountPaid(amountPaid);
            payment.setStatus(amountPaid >= totalCost ? COMPLETED : "In Progress");
        }

        return paymentRepository.save(payment);
    }

    /**
     * distributePayment - Distribute the 'amountPaid' across all sessions in the series
     * in chronological order.
     */
    @Transactional
    public void distributePayment(PaymentEntity payment, Long sessionSeriesId, double amountPaid) {
        var sessions = getSessionsForSeries(sessionSeriesId).stream()
                .sorted(Comparator.comparing(SessionEntity::getSessionTimeStart))
                .toList();

        double remaining = amountPaid;
        double pricePerSession = payment.getGroup().getPrice().getPrice();

        for (SessionEntity session : sessions) {
            if (remaining <= 0) break;

            Optional<PaymentDetailEntity> existingDetailOpt = paymentDetailRepository
                    .findByPaymentIdAndSessionId(payment.getId(), session.getId());

            if (existingDetailOpt.isPresent()) {
                PaymentDetailEntity detail = existingDetailOpt.get();
                double needed = pricePerSession - detail.getAmountPaid();
                if (needed > 0) {
                    double toAdd = Math.min(needed, remaining);
                    detail.setAmountPaid(detail.getAmountPaid() + toAdd);
                    paymentDetailRepository.save(detail);
                    remaining -= toAdd;
                }
            } else {
                double toPay = Math.min(pricePerSession, remaining);
                PaymentDetailEntity newDetail = new PaymentDetailEntity();
                newDetail.setPayment(payment);
                newDetail.setSession(session);
                newDetail.setAmountPaid(toPay);
                paymentDetailRepository.save(newDetail);
                remaining -= toPay;
            }
        }

        // If after distribution, payment is >= total cost => might do final checks
        double totalCost = calculateTotalCost(payment.getGroup());
        if (payment.getAmountPaid() >= totalCost) {
            double surplus = payment.getAmountPaid() - totalCost;
            if (surplus > 0) {
                throw new CustomServiceException(
                        "Le paiement a été complété. Le montant excédentaire de " + surplus + " euros sera remboursé.",
                        HttpStatus.OK
                );
            }
        }
    }

    // -------------------------------------------------
    // Single-session "Catch-up" Payment (rattrapage)
    // -------------------------------------------------

    /**
     * processCatchUpPayment - For a one-off session.
     * The student pays exactly for that session, ignoring the full series.
     */
    @Transactional
    public PaymentEntity processCatchUpPayment(Long studentId, Long sessionId, double amountPaid) {
        StudentEntity student = getStudent(studentId);
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new CustomServiceException(
                        "Session not found with ID: " + sessionId,
                        HttpStatus.BAD_REQUEST
                ));

        double sessionCost = session.getGroup().getPrice().getPrice();
        if (amountPaid > sessionCost) {
            double surplus = amountPaid - sessionCost;
            throw new CustomServiceException(
                    "Le montant payé dépasse le coût de la session de " + surplus + " euros.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Create a PaymentEntity for this single session
        PaymentEntity payment = new PaymentEntity();
        payment.setStudent(student);
        payment.setGroup(session.getGroup());
        payment.setSession(session);  // Link to the single session
        payment.setAmountPaid(amountPaid);
        payment.setStatus(amountPaid >= sessionCost ? COMPLETED : "in progress");
        paymentRepository.save(payment);

        // Create a PaymentDetailEntity for the single session
        PaymentDetailEntity detail = new PaymentDetailEntity();
        detail.setPayment(payment);
        detail.setSession(session);
        detail.setAmountPaid(amountPaid);
        detail.setIsCatchUp(true); // mark as rattrapage
        paymentDetailRepository.save(detail);

        return payment;
    }

    // -------------------------------------------------
    // Helpers & Misc
    // -------------------------------------------------

    private SessionSeriesEntity getSessionSeries(Long seriesId) {
        return sessionSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Series not found with ID: " + seriesId));
    }

    private StudentEntity getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found with ID: " + studentId));
    }

    private GroupEntity getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found with ID: " + groupId));
    }

    private List<SessionEntity> getSessionsForSeries(Long seriesId) {
        var series = getSessionSeries(seriesId);
        return sessionRepository.findBySessionSeries(series);
    }

    private double calculateTotalCost(GroupEntity group) {
        double pricePerSession = group.getPrice().getPrice();
        int sessionNumberPerSerie = group.getSessionNumberPerSerie();
        return pricePerSession * sessionNumberPerSerie;
    }

    private double calculateCreatedSessionsCost(Long seriesId, GroupEntity group) {
        int totalSessions = sessionRepository.countBySessionSeriesId(seriesId);
        double pricePerSession = group.getPrice().getPrice();
        return totalSessions * pricePerSession;
    }

    private boolean canProcessPayment(Long seriesId, double newTotalAmount, GroupEntity group) {
        double totalCreatedCost = calculateCreatedSessionsCost(seriesId, group);
        return newTotalAmount <= totalCreatedCost;
    }

    // --------------------------
    // Payment Overdue & Statuses
    // --------------------------

    @Transactional
    public List<StudentPaymentStatus> getPaymentStatusForGroup(Long groupId) {
        List<StudentPaymentStatus> result = new ArrayList<>();
        GroupEntity group = getGroup(groupId);
        List<StudentEntity> students = studentRepository.findByGroups_Id(groupId);

        for (StudentEntity student : students) {
            boolean isOverdue = isStudentPaymentOverdueForSeries(
                    student.getId(),
                    groupId,
                    group.getPrice().getPrice()
            );

            StudentPaymentStatus paymentStatus = new StudentPaymentStatus(
                    student.getId(),
                    student.getFirstName(),
                    student.getLastName(),
                    student.getGender(),
                    student.getEmail(),
                    student.getPhoneNumber(),
                    student.getDateOfBirth(),
                    student.getPlaceOfBirth(),
                    student.getPhoto(),
                    student.getLevel() != null ? student.getLevel().getId() : null,
                    student.getGroups().stream().map(GroupEntity::getId).collect(Collectors.toSet()),
                    student.getTutor() != null ? student.getTutor().getId() : null,
                    student.getEstablishment(),
                    student.getAverageScore(),
                    student.getActive(),
                    isOverdue
            );
            result.add(paymentStatus);
        }
        return result;
    }

    public boolean isStudentPaymentOverdueForSeries(Long studentId, Long sessionSeriesId, double pricePerSession) {
        long sessionsAttended = attendanceRepository.countByStudentIdAndSessionSeriesIdAndIsPresent(studentId, sessionSeriesId, true);
        double totalDue = sessionsAttended * pricePerSession;

        Double totalPaid = paymentRepository.findAmountPaidForStudentAndSeries(studentId, sessionSeriesId);
        if (totalPaid == null) totalPaid = 0.0;

        return totalPaid < totalDue;
    }

    public List<SessionEntity> getAttendedSessions(Long studentId) {
        return attendanceRepository.findByStudentIdAndIsPresent(studentId, true);
    }

    public Set<SessionEntity> getPaidSessions(Long studentId) {
        List<PaymentDetailEntity> details = paymentDetailRepository.findByPayment_StudentId(studentId);
        return details.stream()
                .map(PaymentDetailEntity::getSession)
                .collect(Collectors.toSet());
    }

    public List<SessionEntity> getUnpaidAttendedSessions(Long studentId) {
        List<SessionEntity> attended = getAttendedSessions(studentId);
        Set<SessionEntity> paid = getPaidSessions(studentId);
        return attended.stream()
                .filter(s -> !paid.contains(s))
                .toList();
    }

    public List<GroupPaymentStatus> getPaymentStatusForStudent(Long studentId) {
        List<GroupPaymentStatus> groupStatuses = new ArrayList<>();
        List<GroupEntity> groups = groupRepository.findByStudents_Id(studentId);

        for (GroupEntity group : groups) {
            List<SeriesPaymentStatus> seriesStatuses = new ArrayList<>();
            List<SessionSeriesEntity> seriesList = sessionSeriesRepository.findByGroupId(group.getId());

            for (SessionSeriesEntity series : seriesList) {
                List<SessionPaymentStatus> sessionStatuses = getSessionPaymentStatuses(studentId, series);
                seriesStatuses.add(new SeriesPaymentStatus(series.getId(), sessionStatuses));
            }
            groupStatuses.add(new GroupPaymentStatus(group.getId(), group.getName(), seriesStatuses));
        }
        return groupStatuses;
    }

    private List<SessionPaymentStatus> getSessionPaymentStatuses(Long studentId, SessionSeriesEntity series) {
        List<SessionPaymentStatus> result = new ArrayList<>();
        List<SessionEntity> sessions = sessionRepository.findBySessionSeries(series);

        for (SessionEntity session : sessions) {
            boolean isOverdue = isPaymentOverdueForSession(studentId, session.getId());
            result.add(new SessionPaymentStatus(session.getId(), session.getTitle(), isOverdue));
        }
        return result;
    }

    private boolean isPaymentOverdueForSession(Long studentId, Long sessionId) {
        List<PaymentDetailEntity> details = paymentDetailRepository.findByPayment_StudentIdAndSessionId(studentId, sessionId);
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));
        double sessionCost = session.getGroup().getPrice().getPrice();

        double totalPaidForSession = details.stream()
                .mapToDouble(PaymentDetailEntity::getAmountPaid)
                .sum();

        return totalPaidForSession < sessionCost;
    }

    // ----------------------------------
    // PaymentDetails / PaymentDTO logic
    // ----------------------------------

    /**
     * Retrieve payment details for a specific series, i.e. each PaymentDetail.
     */
    public List<PaymentDetailDTO> getPaymentDetailsForSeries(Long studentId, Long sessionSeriesId) {
        LOGGER.info("Fetching payment details for studentId={}, seriesId={}", studentId, sessionSeriesId);
        List<PaymentDetailEntity> details = paymentDetailRepository
                .findByPayment_StudentIdAndSession_SessionSeriesId(studentId, sessionSeriesId);
        LOGGER.debug("Payment details retrieved: {}", details);

        return details.stream()
                .map(this::convertToPaymentDetailDto)
                .toList();
    }

    private PaymentDetailDTO convertToPaymentDetailDto(PaymentDetailEntity detail) {
        var dto = PaymentDetailDTO.builder()
                .paymentDetailId(detail.getId())
                .sessionId(detail.getSession().getId())
                .sessionName(detail.getSession().getTitle())
                .amountPaid(detail.getAmountPaid())
                .remainingBalance(detail.getSession().getGroup().getPrice().getPrice() - detail.getAmountPaid())
                .build();
        return dto;
    }

    public List<PaymentDTO> getPaymentHistoryForSeries(Long studentId, Long sessionSeriesId) {
        LOGGER.info("Fetching payment history for studentId={}, seriesId={}", studentId, sessionSeriesId);
        List<PaymentEntity> payments = paymentRepository.findAllByStudentIdAndSessionSeriesId(studentId, sessionSeriesId);
        LOGGER.debug("Payment history retrieved: {}", payments);

        return payments.stream()
                .map(this::convertToDto)
                .toList();
    }

    public PaymentDTO convertToDto(PaymentEntity payment) {
        var dto = PaymentDTO.builder()
                .studentId(payment.getStudent().getId())
                .groupId(payment.getGroup() != null ? payment.getGroup().getId() : null)
                .sessionSeriesId(payment.getSessionSeries() != null ? payment.getSessionSeries().getId() : null)
                .sessionId(payment.getSession() != null ? payment.getSession().getId() : null)

                .amountPaid(payment.getAmountPaid())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDescription(payment.getDescription())

                .totalSeriesCost(calculateTotalSeriesCost(payment))
                .totalPaidForSeries(calculateTotalPaidForSeries(payment))
                .amountOwed(calculateAmountOwed(payment))
                .build();
        return dto;
    }

    private Double calculateTotalSeriesCost(PaymentEntity payment) {
        if (payment.getGroup() == null || payment.getSessionSeries() == null) return 0.0;
        double pricePerSession = payment.getGroup().getPrice().getPrice();
        int sessionCount = payment.getSessionSeries().getSessions().size();
        return pricePerSession * sessionCount;
    }

    private Double calculateTotalPaidForSeries(PaymentEntity payment) {
        return payment.getAmountPaid();
    }

    private Double calculateAmountOwed(PaymentEntity payment) {
        return calculateTotalSeriesCost(payment) - calculateTotalPaidForSeries(payment);
    }
}
