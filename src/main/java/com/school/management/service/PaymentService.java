package com.school.management.service;

import com.school.management.persistance.PaymentEntity;
import com.school.management.repository.AttendanceRepository;
import com.school.management.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;

    private final AttendanceRepository attendanceRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository, AttendanceRepository attendanceRepository) {
        this.paymentRepository = paymentRepository;
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

    public void deletePayment(Long id) {
        paymentRepository.deleteById(id);
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

    public List<PaymentEntity> getAllPaymentsForStudent(Long studentId) {
        return paymentRepository.findAllByStudentIdOrderByPaymentDateDesc(studentId);
    }

    public PaymentEntity save(PaymentEntity payment) {
        return paymentRepository.save(payment);
    }

    // Additional methods as needed...
}
