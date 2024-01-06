package com.school.management.repository;

import com.school.management.persistance.PaymentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<PaymentEntity, Long> {
    // You can add custom query methods here if needed
    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId")
    Double sumPaymentsForStudent(Long studentId);
    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId")
    Double sumPaymentsByStudentId(Long studentId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId AND p.session.id = :sessionId")
    Double sumPaymentsForStudentBySession(Long studentId, Long sessionId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId AND p.session.id = :sessionId AND p.active = true")
    Double sumPaymentsForStudentBySessionAndActive(Long studentId, Long sessionId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId AND p.session.id = :sessionId AND p.active = false")
    Double sumPaymentsForStudentBySessionAndInactive(Long studentId, Long sessionId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId AND p.active = true")
    Double sumPaymentsForStudentAndActive(Long studentId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId AND p.active = false")
    Double sumPaymentsForStudentAndInactive(Long studentId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.session.id = :sessionId AND p.active = true")
    Double sumPaymentsForSessionAndActive(Long sessionId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.session.id = :sessionId AND p.active = false")
    Double sumPaymentsForSessionAndInactive(Long sessionId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.active = true")
    Double sumPaymentsForActive();

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.active = false")
    Double sumPaymentsForInactive();

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.session.id = :sessionId")
    Double sumPaymentsForSession(Long sessionId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p")
    Double sumPayments();

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId AND p.session.id = :sessionId AND p.active = true")
    Double sumPaymentsForStudentBySessionAndActiveAndActive(Long studentId, Long sessionId);

    @Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId AND p.session.id = :sessionId AND p.active = false")
    Double sumPaymentsForStudentBySessionAndInactiveAndActive(Long studentId, Long sessionId);

    //@Query("SELECT SUM(p.amountPaid) FROM PaymentEntity p WHERE p.student.id = :studentId AND p.active = true")
    List<PaymentEntity> findAllByStudentId(Long studentId);

    List<PaymentEntity> findAllByStudentIdOrderByPaymentDateDesc(Long studentId);








}
