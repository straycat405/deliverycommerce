package com.babjo.deliverycommerce.domain.payment.presentation.dto.response;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentMethod;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import com.babjo.deliverycommerce.domain.payment.entity.PgProvider;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 조회 응답 DTO (단건/목록 통합)
 */
@Getter
@Builder
public class PaymentResponse {

    private UUID paymentId;
    private Long userId;
    private UUID orderId;
    private Integer amount;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    private PgProvider pgProvider;
    private String pgPaymentKey;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;
    private Long createdBy;
    private LocalDateTime updatedAt;
    private Long updatedBy;

    public static PaymentResponse from(Payment payment) {
        return PaymentResponse.builder()
                .paymentId(payment.getPaymentId())
                .userId(payment.getUserId())
                .orderId(payment.getOrderId())
                .amount(payment.getAmount())
                .paymentMethod(payment.getPaymentMethod())
                .paymentStatus(payment.getPaymentStatus())
                .pgProvider(payment.getPgProvider())
                .pgPaymentKey(payment.getPgPaymentKey())
                .approvedAt(payment.getApprovedAt())
                .createdAt(payment.getCreatedAt())
                .createdBy(payment.getCreatedBy())
                .updatedAt(payment.getUpdatedAt())
                .updatedBy(payment.getUpdatedBy())
                .build();
    }
}

