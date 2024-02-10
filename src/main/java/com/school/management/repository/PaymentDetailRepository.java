package com.school.management.repository;

import com.school.management.persistance.PaymentDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetailEntity, Long>, JpaSpecificationExecutor<PaymentDetailEntity> {
    Optional<PaymentDetailEntity> findByPaymentIdAndSessionId(Long id, Long id1);

    List<PaymentDetailEntity> findByPayment_StudentId(Long studentId);

    List<PaymentDetailEntity> findByPayment_StudentIdAndSessionId(Long studentId, Long sessionId);
}