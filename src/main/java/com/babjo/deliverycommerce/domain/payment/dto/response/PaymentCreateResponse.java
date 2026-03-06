package com.babjo.deliverycommerce.domain.payment.dto.response;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentMethod;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 생성 응답 DTO (201 Created)
 */
@Getter
@Builder
public class PaymentCreateResponse {

    private UUID paymentId;
    private UUID orderId;
    private PaymentMethod paymentMethod;
    private PaymentStatus status;
    private LocalDateTime createdAt;

    public static PaymentCreateResponse from(Payment payment) {
        return PaymentCreateResponse.builder()
                .paymentId(payment.getPaymentId())
                .orderId(payment.getOrderId())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getPaymentStatus())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}

