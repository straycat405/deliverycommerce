package com.babjo.deliverycommerce.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

public class OrderRequestDto {
    @Getter
    @NoArgsConstructor
    public static class CreateOrder{
        @NotNull(message = "가게 ID는 필수입니다.")
        private UUID storeId;

        @NotBlank(message = "배송지는 필수입니다.")
        private String address;

        private String message;

        @NotEmpty(message = "주문 항목은 1개 이상이어야 합니다.")
        private List<OrderItemRequest> orderItems;
    }

    @Getter
    @NoArgsConstructor
    public static class OrderItemRequest{
        @NotNull(message = "상품 ID는 필수입니다.")
        private UUID productId;

        @NotNull(message = "메뉴 이름은 필수입니다.")
        private String productName;

        @Min(value = 1, message = "수량은 1개 이상이어야 합니다.")
        private Integer orderCount;

        @NotNull(message = "가격 정보가 없습니다.")
        private Integer orderPrice;
    }

    @Getter
    @NoArgsConstructor
    public static class AcceptOrder {
        @Min(value = 5, message = "조리 시간은 최소 5분 이상 입력해야 합니다.")
        private Integer cookingMinutes;
    }

}
