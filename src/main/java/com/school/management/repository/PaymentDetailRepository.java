package com.school.management.repository;

import com.school.management.persistance.PaymentDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface PaymentDetailRepository extends JpaRepository<PaymentDetailEntity, Long>, JpaSpecificationExecutor<PaymentDetailEntity> {
    Optional<PaymentDetailEntity> findByPaymentIdAndSessionId(Long id, Long id1);
}