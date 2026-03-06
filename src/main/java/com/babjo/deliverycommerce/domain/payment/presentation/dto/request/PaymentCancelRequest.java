package com.babjo.deliverycommerce.domain.payment.presentation.dto.request;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 결제 취소 요청 DTO
 * PATCH /v1/payments/{paymentId}/cancel
 * - reason 은 선택사항
 */
@Getter
@NoArgsConstructor
public class PaymentCancelRequest {

    @Size(max = 255, message = "취소 사유는 255자 이하여야 합니다.")
    private String reason;
}

