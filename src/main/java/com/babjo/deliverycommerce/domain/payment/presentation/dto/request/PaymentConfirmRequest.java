package com.babjo.deliverycommerce.domain.payment.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 승인 요청 DTO
 * POST /v1/payments/{paymentId}/confirm
 */
@Getter
@NoArgsConstructor
public class PaymentConfirmRequest {

    @NotBlank(message = "PG 결제 키는 필수입니다.")
    @Size(max = 200, message = "PG 결제 키는 200자 이하여야 합니다.")
    private String pgPaymentKey;
}

