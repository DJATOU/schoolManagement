package com.school.management.service;

import com.school.management.persistance.*;
import com.school.management.repository.*;
import com.school.management.service.exception.CustomServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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


    @Autowired
    public PaymentService(PaymentRepository paymentRepository, StudentRepository studentRepository,
                          GroupRepository groupRepository, PaymentDetailRepository paymentDetailRepository,
                          SessionRepository sessionRepository, AttendanceRepository attendanceRepository){
        this.paymentRepository = paymentRepository;
        this.studentRepository = studentRepository;
        this.groupRepository = groupRepository;
        this.paymentDetailRepository = paymentDetailRepository;
        this.sessionRepository = sessionRepository;
        this.attendanceRepository = attendanceRepository;
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
        var payment = getOrCreatePayment(student, group, seriesId, amountPaid);
        distributePayment(payment, seriesId, amountPaid);

        return paymentRepository.save(payment);
    }

    private StudentEntity getStudent(Long studentId) {
        return studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found"));
    }

    private GroupEntity getGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new RuntimeException("Group not found"));
    }

    private PaymentEntity getOrCreatePayment(StudentEntity student, GroupEntity group, Long seriesId, double amountPaid) {
        var existingPayment = paymentRepository.findByStudentIdAndGroupIdAndSessionSeriesId(student.getId(), group.getId(), seriesId);
        double totalCost = calculateTotalCost(group);

        if (existingPayment.isPresent()) {
            return getPaymentEntity(amountPaid, existingPayment, totalCost);
        } else {
            if (amountPaid > totalCost) {
                // Si le premier paiement dépasse le coût total, lancez une exception.
                throw new CustomServiceException("Le montant du paiement ne peut pas dépasser le coût total de la série.");
            }

            PaymentEntity newPayment = new PaymentEntity();
            newPayment.setStudent(student);
            newPayment.setGroup(group);
            newPayment.setAmountPaid(amountPaid);
            newPayment.setStatus(amountPaid >= totalCost ? "Completed" : "In Progress");
            return newPayment;
        }
    }

    private static PaymentEntity getPaymentEntity(double amountPaid, Optional<PaymentEntity> existingPayment, double totalCost) {
        if(existingPayment.isEmpty()) {
            throw new CustomServiceException("Pyament NOT FOUND", HttpStatus.NOT_FOUND);
        }
        PaymentEntity payment = existingPayment.get();
        double newTotalAmount = payment.getAmountPaid() + amountPaid;

        if (newTotalAmount > totalCost) {
            // Si le nouveau montant total dépasse le coût total, lancez une exception.
            throw new CustomServiceException("Le montant total des paiements a déjà couvert le coût total de la série.", HttpStatus.BAD_REQUEST);
        }

        payment.setAmountPaid(newTotalAmount);
        payment.setStatus(newTotalAmount >= totalCost ? "Completed" : "In Progress");
        return payment;
    }


    private void distributePayment(PaymentEntity payment, Long seriesId, double amountPaid) {
        if (payment.getStatus().equals("Completed")) {
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
        // Implement the logic to retrieve sessions that are part of the specified series.
        // This might involve querying your session repository for sessions linked to the series.
        return sessionRepository.findBySessionSeriesId(seriesId);
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
                return remainingAmount - amountToAdd;
            }
        }

        else if (remainingAmount > 0) {
            double amountToPay = Math.min(pricePerSession, remainingAmount);
            var newDetail = new PaymentDetailEntity();
            newDetail.setAmountPaid(amountToPay);
            newDetail.setPayment(payment);
            newDetail.setSession(session);
            paymentDetailRepository.save(newDetail);
            return remainingAmount - amountToPay;
        }
        return remainingAmount;

    }

    public double calculateAmountDue(Long studentId, double pricePerSession) {
        long numberOfSessionsAttended = attendanceRepository.countByStudentIdAndIsPresent(studentId, true);
        return pricePerSession * numberOfSessionsAttended;
    }

    public boolean isStudentPaymentOverdue(Long studentId, double pricePerSession) {
        double totalDue = calculateAmountDue(studentId, pricePerSession);
        Double totalPaid = paymentRepository.sumPaymentsForStudent(studentId);
        if (totalPaid == null) {
            totalPaid = 0.0; // Si aucun paiement n'a été effectué
        }
        return totalPaid < totalDue;
    }

    public double calculateAmountDueForSeries(Long studentId, Long seriesId, double pricePerSession) {
        long numberOfSessionsAttended = attendanceRepository.countByStudentIdAndSessionSeriesIdAndIsPresent(studentId, seriesId, true);
        return pricePerSession * numberOfSessionsAttended;
    }

    public boolean isStudentPaymentOverdueForSeries(Long studentId, Long seriesId, double pricePerSession) {
        double totalDueForSeries = calculateAmountDueForSeries(studentId, seriesId, pricePerSession);
        Double totalPaidForSeries = paymentRepository.findAmountPaidForStudentAndSeries(studentId, seriesId);
        if (totalPaidForSeries == null) {
            totalPaidForSeries = 0.0;
        }
        return totalPaidForSeries < totalDueForSeries;
    }




}