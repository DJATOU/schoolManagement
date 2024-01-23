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
    var payment = getOrCreatePayment(student, group, series, amountPaid, seriesId); // Pass seriesId to getOrCreatePayment

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

    if (existingPayment.isPresent()) {
        return getPaymentEntity(amountPaid, existingPayment, totalCost, seriesId);
    } else {
        if (amountPaid > totalCost) {
            // Si le premier paiement dépasse le coût total, lancez une exception.
            throw new CustomServiceException("Le montant du paiement ne peut pas dépasser le coût total de la série.");
        }

        PaymentEntity newPayment = new PaymentEntity();
        newPayment.setStudent(student);
        newPayment.setGroup(group);
        newPayment.setSessionSeries(series);
        newPayment.setAmountPaid(amountPaid);
        newPayment.setStatus(amountPaid >= totalCost ? completed : "In Progress");
        return newPayment;
    }
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
        if (payment.getStatus().equals(completed)) {
            throw new CustomServiceException("Le paiement pour cette série est déjà complet. Aucune mise à jour supplémentaire n'est autorisée.", HttpStatus.BAD_REQUEST);
        }
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
                return remainingAmount - amountToAdd;
            }
        }

        else if (remainingAmount > 0) {
            double amountToPay = Math.min(pricePerSession, remainingAmount);
            var newDetail = new PaymentDetailEntity();
            newDetail.setAmountPaid(amountToPay);
            newDetail.setPayment(payment);
            newDetail.setSession(session);
            payment.getPaymentDetails().add(newDetail);
            return remainingAmount - amountToPay;
        }
        return remainingAmount;
    }

    // Dans PaymentService.java
    @Transactional
    public List<StudentPaymentStatus> getPaymentStatusForGroup(Long groupId) {
        List<StudentPaymentStatus> paymentStatusList = new ArrayList<>();
        List<StudentEntity> students = studentRepository.findByGroups_Id(groupId);
        GroupEntity group = getGroup(groupId);
        for (StudentEntity student : students) {
            boolean isOverdue = isStudentPaymentOverdueForSeries(student.getId(), groupId, group.getPrice().getPrice());
            paymentStatusList.add(new StudentPaymentStatus(student.getId(), student.getFirstName(), isOverdue));
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



}