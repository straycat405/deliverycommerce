package com.babjo.deliverycommerce.domain.payment.dto.response;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 취소 응답 DTO (200 OK)
 */
@Getter
@Builder
public class PaymentCancelResponse {

    private UUID paymentId;
    private PaymentStatus status;
    private LocalDateTime canceledAt;

    public static PaymentCancelResponse from(Payment payment) {
        return PaymentCancelResponse.builder()
                .paymentId(payment.getPaymentId())
                .status(payment.getPaymentStatus())
                .canceledAt(payment.getUpdatedAt()) // 취소 시각은 updatedAt 활용
                .build();
    }
}

