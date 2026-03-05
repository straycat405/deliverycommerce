package com.babjo.deliverycommerce.domain.order.dto;

import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Getter
@Builder
@AllArgsConstructor
public class OrderResponseDto {
    private UUID orderId;
    private Long userId;
    private UUID storeId;
    private OrderStatus status;
    private Integer totalPrice;
    private String address;
    private String message;
    private List<OrderItemResponse> orderItems;
    private LocalDateTime createdAt;
    private LocalDateTime canceledAt;
    private Long canceledBy;
    private String cancelReason;
    private Long acceptedBy;
    private Integer cookingMinutes;
    private Long preparingStartedBy;
    private Long pickupReadieBy;
    private Long pickupBy;
    private LocalDateTime acceptedAt;


    @Getter
    @Builder
    @AllArgsConstructor
    public static class OrderItemResponse{
        private Long id;
        private UUID productId;
        private String productName;
        private Integer orderPrice;
        private Integer orderCount;

    }
}
