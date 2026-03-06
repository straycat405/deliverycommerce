package com.babjo.deliverycommerce.domain.payment.repository;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;

import java.util.List;
import java.util.UUID;

public interface PaymentRepositoryCustom {

    /**
     * 조건 기반 동적 목록 조회 (QueryDSL)
     * - paymentId: 단건 조회 용
     * - orderId: 주문별 조회
     * - paymentStatus: 상태별 필터
     * - userId: 본인 결제만 조회 (null이면 전체 - 관리자용)
     */
    List<Payment> searchPayments(UUID paymentId, UUID orderId, PaymentStatus paymentStatus, Long userId);
}

