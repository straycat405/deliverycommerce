package com.babjo.deliverycommerce.domain.payment.presentation.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.UUID;

/**
 * 결제 삭제(Soft Delete) 응답 DTO (200 OK)
 */
@Getter
@Builder
public class PaymentDeleteResponse {

    private UUID paymentId;
    private boolean deleted;

    public static PaymentDeleteResponse of(UUID paymentId) {
        return PaymentDeleteResponse.builder()
                .paymentId(paymentId)
                .deleted(true)
                .build();
    }
}

