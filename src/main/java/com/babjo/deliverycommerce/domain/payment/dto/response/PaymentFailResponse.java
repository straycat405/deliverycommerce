package com.babjo.deliverycommerce.domain.payment.dto.response;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 실패 응답 DTO (200 OK)
 */
@Getter
@Builder
public class PaymentFailResponse {

    private UUID paymentId;
    private PaymentStatus status;
    private LocalDateTime updatedAt;

    public static PaymentFailResponse from(Payment payment) {
        return PaymentFailResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getPaymentStatus())
                .updatedAt(payment.getUpdatedAt())
                .build();
    }
}

