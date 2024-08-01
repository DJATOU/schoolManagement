package com.school.management.service;

import com.school.management.persistance.*;
import com.school.management.repository.*;
import com.school.management.service.exception.CustomServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final StudentRepository studentRepository;

    private final GroupRepository groupRepository;

    private final PaymentDetailRepository paymentDetailRepository;

    private final SessionRepository sessionRepository;

    private final AttendanceRepository attendanceRepository;

    private final SessionSeriesRepository sessionSeriesRepository;

    private static final String completed = "completed" ;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, StudentRepository studentRepository,
                          GroupRepository groupRepository, PaymentDetailRepository paymentDetailRepository,
                          SessionRepository sessionRepository, AttendanceRepository attendanceRepository
                          , SessionSeriesRepository sessionSeriesRepository) {
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
                .orElseThrow(() -> new RuntimeException("Payment not found")); // Customize this exception
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

    public PaymentEntity processPayment(Long studentId, Long groupId, Long seriesId, double amountPaid) {
        var student = getStudent(studentId);
        var group = getGroup(groupId);
        var series = getSessionSeries(seriesId);
        var payment = getOrCreatePayment(student, group, series, amountPaid, seriesId);

        // Update the status of the payment after distribution
        payment.setStatus(payment.getAmountPaid() >= calculateTotalCost(group) ? completed : "In Progress");
        return paymentRepository.save(payment);
    }

    private SessionSeriesEntity getSessionSeries(Long seriesId) {
        return sessionSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Series not found with ID: " + seriesId));
    }

    private StudentEntity getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    private GroupEntity getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    private PaymentEntity getOrCreatePayment(StudentEntity student, GroupEntity group, SessionSeriesEntity series, double amountPaid, Long seriesId) {
        var existingPayment = paymentRepository.findByStudentIdAndGroupIdAndSessionSeriesId(student.getId(), group.getId(), series.getId());
        double totalCost = calculateTotalCost(group);
        double surplus = 0.0;
        PaymentEntity payment;

        if (existingPayment.isPresent()) {
            payment = existingPayment.get();
            double newTotalAmount = payment.getAmountPaid() + amountPaid;

            if (newTotalAmount > totalCost) {
                surplus = newTotalAmount - totalCost;
                amountPaid = totalCost - payment.getAmountPaid(); // Ajuster le montant à distribuer
                payment.setAmountPaid(totalCost);
                payment.setStatus(completed);
            } else {
                payment.setAmountPaid(newTotalAmount);
            }
        } else {
            payment = new PaymentEntity();
            payment.setStudent(student);
            payment.setGroup(group);
            payment.setSessionSeries(series);
            payment.setAmountPaid(Math.min(amountPaid, totalCost));
            payment.setStatus(amountPaid >= totalCost ? completed : "In Progress");
            amountPaid = Math.min(amountPaid, totalCost);
        }

        paymentRepository.save(payment);

        // Distribuer le paiement après avoir mis à jour l'entité de paiement
        distributePayment(payment, seriesId, amountPaid);

        if (surplus > 0) {
            throw new CustomServiceException("Le paiement a été complété. Le montant excédentaire de " + surplus + " euros sera remboursé.", HttpStatus.OK);
        }

        return payment;
    }


    private PaymentEntity getPaymentEntity(double amountPaid, Optional<PaymentEntity> existingPayment, double totalCost, Long seriesId) {
    if(existingPayment.isEmpty()) {
        throw new CustomServiceException("Payment NOT FOUND", HttpStatus.NOT_FOUND);
    }
    PaymentEntity payment = existingPayment.get();
    double newTotalAmount = payment.getAmountPaid() + amountPaid;

    if (newTotalAmount > totalCost) {
        // Si le nouveau montant total dépasse le coût total, lancez une exception.
        throw new CustomServiceException("Le montant total des paiements a déjà couvert le coût total de la série.", HttpStatus.BAD_REQUEST);
    }

    payment.setAmountPaid(newTotalAmount);
    distributePayment(payment, seriesId, amountPaid); // Distribute the payment before setting the status

    payment.setStatus(newTotalAmount >= totalCost ? completed : "In Progress");
    return payment;
    }

    private void distributePayment(PaymentEntity payment, Long seriesId, double amountPaid) {
        var sessions = getSessionsForSeries(seriesId);
        var remainingAmount = amountPaid;

        for (var session : sessions) {
            remainingAmount = updateOrAddPaymentDetail(payment, session, remainingAmount);
            if (remainingAmount <= 0) break;
        }
    }


    private List<SessionEntity> getSessionsForSeries(Long seriesId) {
        SessionSeriesEntity series = sessionSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new RuntimeException("Series not found with ID: " + seriesId));
        return sessionRepository.findBySessionSeries(series);
    }

    private double calculateTotalCost(GroupEntity group) {
        return group.getPrice().getPrice() * group.getSessionNumberPerSerie();
    }

    private double updateOrAddPaymentDetail(PaymentEntity payment, SessionEntity session, double remainingAmount) {
        double pricePerSession = payment.getGroup().getPrice().getPrice();
        var existingDetailOpt = paymentDetailRepository.findByPaymentIdAndSessionId(payment.getId(), session.getId());

        if (existingDetailOpt.isPresent()) {
            var existingDetail = existingDetailOpt.get();
            double amountToAdd = Math.min(pricePerSession - existingDetail.getAmountPaid(), remainingAmount);
            if (amountToAdd > 0) {
                existingDetail.setAmountPaid(existingDetail.getAmountPaid() + amountToAdd);
                paymentDetailRepository.save(existingDetail);
                remainingAmount -= amountToAdd;
            }
        } else if (remainingAmount > 0) {
            double amountToPay = Math.min(pricePerSession, remainingAmount);
            var newDetail = new PaymentDetailEntity();
            newDetail.setAmountPaid(amountToPay);
            newDetail.setPayment(payment);
            newDetail.setSession(session);
            payment.getPaymentDetails().add(newDetail);
            paymentDetailRepository.save(newDetail);
            remainingAmount -= amountToPay;
        }
        return remainingAmount;
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
                    student.getLevel(),
                    student.getGroups().stream().map(GroupEntity::getId).collect(Collectors.toSet()),
                    student.getTutor().getId(),
                    student.getEstablishment(),
                    student.getAverageScore(),
                    isOverdue
            );

            paymentStatusList.add(paymentStatus);
        }

        return paymentStatusList;
    }


    public boolean isStudentPaymentOverdueForSeries(Long studentId, Long seriesId, double pricePerSession) {
        // Calcule le montant total dû pour toutes les séances auxquelles l'étudiant a assisté dans la série spécifique
        long numberOfSessionsAttended = attendanceRepository.countByStudentIdAndSessionSeriesIdAndIsPresent(studentId, seriesId, true);
        double totalDueForSeries = pricePerSession * numberOfSessionsAttended;

        // Trouve le montant total que l'étudiant a payé pour cette série
        Double totalPaidForSeries = paymentRepository.findAmountPaidForStudentAndSeries(studentId, seriesId);
        if (totalPaidForSeries == null) {
            totalPaidForSeries = 0.0; // Aucun paiement n'a été trouvé pour cette série
        }

        // Compare le montant dû au montant payé pour déterminer si l'étudiant est en retard
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
        // Récupérer les détails du paiement pour l'étudiant et la session spécifiée
        List<PaymentDetailEntity> paymentDetails = paymentDetailRepository.findByPayment_StudentIdAndSessionId(studentId, sessionId);

        // Récupérer le coût de la session
        SessionEntity session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Session not found with ID: " + sessionId));
        double sessionCost = session.getGroup().getPrice().getPrice(); // Assurez-vous que cette chaîne d'appels est correcte selon votre modèle

        // Calculer le montant total payé pour la session
        double totalPaidForSession = paymentDetails.stream()
                .mapToDouble(PaymentDetailEntity::getAmountPaid)
                .sum();

        // Comparer le montant total payé au coût de la session
        return totalPaidForSession < sessionCost;
    }
}