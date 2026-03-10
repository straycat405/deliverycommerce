package com.babjo.deliverycommerce.domain.payment.repository;

import com.babjo.deliverycommerce.domain.payment.entity.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, UUID> {

    List<PaymentHistory> findAllByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
}

