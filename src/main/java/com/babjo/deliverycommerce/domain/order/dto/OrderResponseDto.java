package com.babjo.deliverycommerce.domain.order.dto;

import com.babjo.deliverycommerce.domain.order.entity.Order;
import com.babjo.deliverycommerce.domain.order.entity.OrderItem;
import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
import com.fasterxml.jackson.annotation.JsonInclude;
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
    private LocalDateTime acceptedAt;
    private Integer cookingMinutes;
    private Long preparingStartedBy;
    private LocalDateTime preparingStartedAt;
    private Long pickupReadieBy;
    private LocalDateTime pickupReadieAt;
    private Long pickupBy;
    private LocalDateTime pickupAt;


    // 주문 생성, 상세 조회
    @Getter
    @Builder
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class OrderDetail{
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
        private LocalDateTime acceptedAt;
        private String cancelReason;
        private Integer cookingMinutes;
        private LocalDateTime preparingStartedAt;
        private LocalDateTime pickupReadieAt;
        private LocalDateTime pickupAt;

        public static OrderDetail from(Order order){
            return OrderDetail.builder()
                    .orderId(order.getOrderId())
                    .userId(order.getUserId())
                    .storeId(order.getStoreId())
                    .status(order.getStatus())
                    .totalPrice(order.getTotalPrice())
                    .address(order.getAddress())
                    .message(order.getMessage())
                    .orderItems(order.getOrderItems().stream()
                            .map(OrderItemResponse::from)
                            .toList())
                    .createdAt(order.getCreatedAt())
                    .acceptedAt(order.getAcceptedAt())
                    .canceledAt(order.getCanceledAt())
                    .cancelReason(order.getCancelReason())
                    .cookingMinutes(order.getCookingMinutes())
                    .build();
        }
    }

    // 주문 상품 조회
    @Getter
    @Builder
    public static class OrderItemResponse {
        private Long id;
        private UUID productId;
        private String productName;
        private Integer orderPrice;
        private Integer orderCount;

        public static OrderItemResponse from(OrderItem item){
            return OrderItemResponse.builder()
                    .id(item.getId())
                    .productId(item.getProductId())
                    .productName(item.getProductName())
                    .orderPrice(item.getOrderPrice())
                    .orderCount(item.getOrderCount())
                    .build();
        }
    }
    // 주문 목록 조회
    @Getter @Builder
    public static class OrderList {
        private UUID orderId;
        private OrderStatus status;
        private Integer totalPrice;
        private LocalDateTime createdAt;

        public static OrderList from(Order order) {
            return OrderList.builder()
                    .orderId(order.getOrderId())
                    .status(order.getStatus())
                    .totalPrice(order.getTotalPrice())
                    .createdAt(order.getCreatedAt())
                    .build();
        }
    }

    // 상태 변경 응답
    @Getter @Builder
    public static class OrderAction {
        private UUID orderId;
        private OrderStatus status;
        private LocalDateTime updatedAt;

        public static OrderAction from(Order order, LocalDateTime updatedAt) {
            return OrderAction.builder()
                    .orderId(order.getOrderId())
                    .status(order.getStatus())
                    .updatedAt(updatedAt)
                    .build();
        }
    }
}
