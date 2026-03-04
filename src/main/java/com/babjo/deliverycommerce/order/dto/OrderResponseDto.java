package com.babjo.deliverycommerce.order.dto;

import com.babjo.deliverycommerce.order.entity.OrderStatus;
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
    private LocalDateTime acceptedAt;
    private LocalDateTime canceledAt;
    private Long canceledBy;
    private String cancelReason;

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
