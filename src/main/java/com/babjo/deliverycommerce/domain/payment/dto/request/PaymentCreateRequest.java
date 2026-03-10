package com.babjo.deliverycommerce.domain.payment.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * 결제 생성 요청 DTO
 * POST /v1/payments
 */
@Getter
@NoArgsConstructor
public class PaymentCreateRequest {

    @NotNull(message = "주문 ID는 필수입니다.")
    private UUID orderId;

    @NotNull(message = "결제 금액은 필수입니다.")
    @Min(value = 1, message = "결제 금액은 1원 이상이어야 합니다.")
    private Integer amount;
}

