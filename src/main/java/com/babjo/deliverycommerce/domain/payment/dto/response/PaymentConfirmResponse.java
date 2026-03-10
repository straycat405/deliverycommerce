package com.babjo.deliverycommerce.domain.payment.dto.response;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 승인 응답 DTO (200 OK)
 */
@Getter
@Builder
public class PaymentConfirmResponse {

    private UUID paymentId;
    private PaymentStatus status;
    private LocalDateTime approvedAt;

    public static PaymentConfirmResponse from(Payment payment) {
        return PaymentConfirmResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getPaymentStatus())
                .approvedAt(payment.getApprovedAt())
                .build();
    }
}

