package com.school.management.service;

import com.school.management.dto.PaymentDTO;
import com.school.management.dto.PaymentDetailDTO;
import com.school.management.persistance.*;
import com.school.management.repository.*;
import com.school.management.service.exception.CustomServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class PaymentService {
    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    private final PaymentRepository paymentRepository;
    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;
    private final PaymentDetailRepository paymentDetailRepository;
    private final SessionRepository sessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final SessionSeriesRepository sessionSeriesRepository;

    private static final String COMPLETED = "completed";

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, StudentRepository studentRepository,
                          GroupRepository groupRepository, PaymentDetailRepository paymentDetailRepository,
                          SessionRepository sessionRepository, AttendanceRepository attendanceRepository,
                          SessionSeriesRepository sessionSeriesRepository) {
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.paymentDetailRepository = paymentDetailRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.sessionSeriesRepository = sessionSeriesRepository;
    }

    public List<PaymentEntity> getAllPayments() {
        return paymentRepository.findAll();
    }

    public PaymentEntity getPaymentById(Long id) {
        return paymentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Payment not found"));
    }

    public PaymentEntity createPayment(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    public PaymentEntity updatePayment(Long id) {
        PaymentEntity existingPayment = getPaymentById(id);
        // Update fields of existingPayment
        // ...
        return paymentRepository.save(existingPayment);
    }

    public List<PaymentEntity> getAllPaymentsForStudent(Long studentId) {
        return paymentRepository.findAllByStudentIdOrderByPaymentDateDesc(studentId);
    }

    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    public PaymentEntity processPayment(Long studentId, Long groupId, Long sessionSeriesId, double amountPaid) {
        StudentEntity student = getStudent(studentId);
        GroupEntity group = getGroup(groupId);
        SessionSeriesEntity series = getSessionSeries(sessionSeriesId);

        // Récupérer le coût total de la série
        double totalSeriesCost = calculateTotalCost(group);

        // Récupérer le paiement existant s'il y en a un
        Optional<PaymentEntity> existingPaymentOpt = paymentRepository.findByStudentIdAndGroupIdAndSessionSeriesId(studentId, groupId, sessionSeriesId);
        double currentTotalPaid = existingPaymentOpt.map(PaymentEntity::getAmountPaid).orElse(0.0);
        double newTotalAmount = currentTotalPaid + amountPaid;

        // Vérifier si le nouveau total payé dépasse le coût total de la série
        if (newTotalAmount > totalSeriesCost) {
            double surplus = newTotalAmount - totalSeriesCost;
            throw new CustomServiceException(
                    "Le montant payé dépasse le coût total de la série de " + surplus + " euros.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Vérifier si le paiement dépasse le coût des sessions créées
        if (!canProcessPayment(sessionSeriesId, newTotalAmount, group)) {
            throw new CustomServiceException(
                    "Le paiement ne peut pas être effectué car il dépasse le coût des sessions actuellement créées.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Continuer avec le processus de paiement si valide
        PaymentEntity payment = getOrCreatePayment(student, group, series, amountPaid, sessionSeriesId);

        // Mettre à jour le statut du paiement après distribution
        return paymentRepository.save(payment);
    }


    private boolean canProcessPayment(Long sessionSeriesId, double totalProposedAmount, GroupEntity group) {
        double totalCreatedSessionsCost = calculateCreatedSessionsCost(sessionSeriesId, group);

        // Vérifier si le montant total proposé est supérieur au coût total des sessions créées
        return totalProposedAmount <= totalCreatedSessionsCost;
    }


    private double calculateCreatedSessionsCost(Long sessionSeriesId, GroupEntity group) {
        // Comptez directement les sessions associées à la série
        int totalSessions = sessionRepository.countBySessionSeriesId(sessionSeriesId);
        double pricePerSession = group.getPrice().getPrice();

        return totalSessions * pricePerSession;
    }

    private SessionSeriesEntity getSessionSeries(Long sessionSeriesId) {
        return sessionSeriesRepository.findById(sessionSeriesId)
                .orElseThrow(() -> new RuntimeException("Series not found with ID: " + sessionSeriesId));
    }

    private StudentEntity getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    private GroupEntity getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    private PaymentEntity getOrCreatePayment(StudentEntity student, GroupEntity group, SessionSeriesEntity series, double amountPaid, Long sessionSeriesId) {
        Optional<PaymentEntity> existingPaymentOpt = paymentRepository.findByStudentIdAndGroupIdAndSessionSeriesId(student.getId(), group.getId(), series.getId());
        double totalCost = calculateTotalCost(group); // Coût total de la série
        double surplus;
        PaymentEntity payment;

        if (existingPaymentOpt.isPresent()) {
            payment = existingPaymentOpt.get();
            double newTotalAmount = payment.getAmountPaid() + amountPaid;

            double createdSessionsCost = calculateCreatedSessionsCost(sessionSeriesId, group);

            // Vérifier si le nouveau total dépasse le coût des sessions créées
            if (newTotalAmount > createdSessionsCost) {
                throw new CustomServiceException("Le paiement total dépasse le coût des sessions créées.", HttpStatus.BAD_REQUEST);
            }

            payment.setAmountPaid(newTotalAmount);
            payment.setStatus(newTotalAmount >= totalCost ? COMPLETED : "In Progress");
        } else {
            payment = new PaymentEntity();
            payment.setStudent(student);
            payment.setGroup(group);
            payment.setSessionSeries(series);
            payment.setAmountPaid(amountPaid);
            payment.setStatus(amountPaid >= totalCost ? COMPLETED : "In Progress");
        }

        paymentRepository.save(payment);

        distributePayment(payment, sessionSeriesId, amountPaid);

        if (payment.getAmountPaid() >= totalCost) {
            surplus = payment.getAmountPaid() - totalCost;
            if (surplus > 0) {
                throw new CustomServiceException("Le paiement a été complété. Le montant excédentaire de " + surplus + " euros sera remboursé.", HttpStatus.OK);
            }
        }

        return payment;
    }


    private void distributePayment(PaymentEntity payment, Long sessionSeriesId, double amountPaid) {
        List<SessionEntity> sessions = getSessionsForSeries(sessionSeriesId);

        sessions = sessions.stream()
                .sorted(Comparator.comparing(SessionEntity::getSessionTimeStart))
                .toList();

        double remainingAmount = amountPaid;

        for (SessionEntity session : sessions) {
            if (remainingAmount <= 0) break;

            double pricePerSession = payment.getGroup().getPrice().getPrice();
            Optional<PaymentDetailEntity> existingDetailOpt = paymentDetailRepository.findByPaymentIdAndSessionId(payment.getId(), session.getId());

            if (existingDetailOpt.isPresent()) {
                PaymentDetailEntity existingDetail = existingDetailOpt.get();
                double amountNeeded = pricePerSession - existingDetail.getAmountPaid();

                if (amountNeeded > 0) {
                    double amountToAdd = Math.min(amountNeeded, remainingAmount);
                    existingDetail.setAmountPaid(existingDetail.getAmountPaid() + amountToAdd);
                    paymentDetailRepository.save(existingDetail);
                    remainingAmount -= amountToAdd;
                }
            } else {
                double amountToPay = Math.min(pricePerSession, remainingAmount);
                PaymentDetailEntity newDetail = new PaymentDetailEntity();
                newDetail.setAmountPaid(amountToPay);
                newDetail.setPayment(payment);
                newDetail.setSession(session);
                paymentDetailRepository.save(newDetail);
                remainingAmount -= amountToPay;
            }
        }
    }


    private List<SessionEntity> getSessionsForSeries(Long sessionSeriesId) {
        SessionSeriesEntity series = sessionSeriesRepository.findById(sessionSeriesId)
                .orElseThrow(() -> new RuntimeException("Series not found with ID: " + sessionSeriesId));
        return sessionRepository.findBySessionSeries(series);
    }

    private double calculateTotalCost(GroupEntity group) {
        return group.getPrice().getPrice() * group.getSessionNumberPerSerie();
    }

    @Transactional
    public List<StudentPaymentStatus> getPaymentStatusForGroup(Long groupId) {
        List<StudentPaymentStatus> paymentStatusList = new ArrayList<>();
        List<StudentEntity> students = studentRepository.findByGroups_Id(groupId);
        GroupEntity group = getGroup(groupId);

        for (StudentEntity student : students) {
            boolean isOverdue = isStudentPaymentOverdueForSeries(student.getId(), groupId, group.getPrice().getPrice());

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
                    student.getLevel().getId(),
                    student.getGroups().stream().map(GroupEntity::getId).collect(Collectors.toSet()),
                    student.getTutor().getId(),
                    student.getEstablishment(),
                    student.getAverageScore(),
                    student.getActive(),
                    isOverdue
            );

            paymentStatusList.add(paymentStatus);
        }

        return paymentStatusList;
    }

    public boolean isStudentPaymentOverdueForSeries(Long studentId, Long sessionSeriesId, double pricePerSession) {
        long numberOfSessionsAttended = attendanceRepository.countByStudentIdAndSessionSeriesIdAndIsPresent(studentId, sessionSeriesId, true);
        double totalDueForSeries = pricePerSession * numberOfSessionsAttended;

        Double totalPaidForSeries = paymentRepository.findAmountPaidForStudentAndSeries(studentId, sessionSeriesId);
        if (totalPaidForSeries == null) {
            totalPaidForSeries = 0.0;
        }

        return totalPaidForSeries < totalDueForSeries;
    }

    public List<SessionEntity> getAttendedSessions(Long studentId) {
        return attendanceRepository.findByStudentIdAndIsPresent(studentId, true);
    }

    public Set<SessionEntity> getPaidSessions(Long studentId) {
        List<PaymentDetailEntity> paymentDetails = paymentDetailRepository.findByPayment_StudentId(studentId);
        return paymentDetails.stream()
                .map(PaymentDetailEntity::getSession)
                .collect(Collectors.toSet());
    }

    public List<SessionEntity> getUnpaidAttendedSessions(Long studentId) {
        List<SessionEntity> attendedSessions = getAttendedSessions(studentId);
        Set<SessionEntity> paidSessions = getPaidSessions(studentId);

        return attendedSessions.stream()
                .filter(session -> !paidSessions.contains(session))
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
        List<SessionPaymentStatus> sessionStatuses = new ArrayList<>();
        List<SessionEntity> sessions = sessionRepository.findBySessionSeries(series);

        for (SessionEntity session : sessions) {
            boolean isOverdue = isPaymentOverdueForSession(studentId, session.getId());
            sessionStatuses.add(new SessionPaymentStatus(session.getId(), session.getTitle(), isOverdue));
        }

        return sessionStatuses;
    }

    private boolean isPaymentOverdueForSession(Long studentId, Long sessionId) {
        List<PaymentDetailEntity> paymentDetails = paymentDetailRepository.findByPayment_StudentIdAndSessionId(studentId, sessionId);

        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));
        double sessionCost = session.getGroup().getPrice().getPrice();

        double totalPaidForSession = paymentDetails.stream()
                .mapToDouble(PaymentDetailEntity::getAmountPaid)
                .sum();

        return totalPaidForSession < sessionCost;
    }



    // Method to retrieve payment details for a specific series
    public List<PaymentDetailDTO> getPaymentDetailsForSeries(Long studentId, Long sessionSeriesId) {
        logger.info("Fetching payment details for student ID: {} and series ID: {}", studentId, sessionSeriesId);
        List<PaymentDetailEntity> paymentDetails = paymentDetailRepository.findByPayment_StudentIdAndSession_SessionSeriesId(studentId, sessionSeriesId);
        logger.debug("Payment details retrieved: {}", paymentDetails);
        return paymentDetails.stream()
                .map(this::convertToPaymentDetailDto)
                .toList();
    }

    private PaymentDetailDTO convertToPaymentDetailDto(PaymentDetailEntity detail) {
        logger.debug("Converting PaymentDetailEntity to PaymentDetailDTO for detail ID: {}", detail.getId());
        PaymentDetailDTO dto = PaymentDetailDTO.builder()
                .paymentDetailId(detail.getId())
                .sessionId(detail.getSession().getId())
                .sessionName(detail.getSession().getTitle())
                .amountPaid(detail.getAmountPaid())
                .remainingBalance(detail.getSession().getGroup().getPrice().getPrice() - detail.getAmountPaid())
                .build();
        logger.debug("Converted PaymentDetailDTO: {}", dto);
        return dto;
    }

    public List<PaymentDTO> getPaymentHistoryForSeries(Long studentId, Long sessionSeriesId) {
        logger.info("Fetching payment history for student ID: {} and series ID: {}", studentId, sessionSeriesId);
        List<PaymentEntity> payments = paymentRepository.findAllByStudentIdAndSessionSeriesId(studentId, sessionSeriesId);
        logger.debug("Payment history retrieved: {}", payments);
        return payments.stream()
                .map(this::convertToDto)
                .toList();
    }

    private PaymentDTO convertToDto(PaymentEntity payment) {
        logger.debug("Converting PaymentEntity to PaymentDTO for payment ID: {}", payment.getId());
        PaymentDTO dto = PaymentDTO.builder()
                .studentId(payment.getStudent().getId())
                .sessionSeriesId(payment.getSessionSeries().getId())
                .amountPaid(payment.getAmountPaid())
                .status(payment.getStatus())
                .paymentMethod(payment.getPaymentMethod())
                .paymentDescription(payment.getDescription())
                .totalSeriesCost(calculateTotalSeriesCost(payment))
                .totalPaidForSeries(calculateTotalPaidForSeries(payment))
                .amountOwed(calculateAmountOwed(payment))
                .build();
        logger.debug("Converted PaymentDTO: {}", dto);
        return dto;
    }
    // Calculate the total cost for the series
    private Double calculateTotalSeriesCost(PaymentEntity payment) {
        double pricePerSession = payment.getGroup().getPrice().getPrice();
        return pricePerSession * payment.getSessionSeries().getSessions().size();
    }

    // Calculate the total amount paid for the series
    private Double calculateTotalPaidForSeries(PaymentEntity payment) {
        return payment.getAmountPaid();
    }

    // Calculate the remaining amount owed for the series
    private Double calculateAmountOwed(PaymentEntity payment) {
        return calculateTotalSeriesCost(payment) - calculateTotalPaidForSeries(payment);
    }

}
