package com.babjo.deliverycommerce.order.service;

import com.babjo.deliverycommerce.order.dto.OrderRequestDto;
import com.babjo.deliverycommerce.order.dto.OrderResponseDto;
import com.babjo.deliverycommerce.order.entity.Order;
import com.babjo.deliverycommerce.order.entity.OrderItem;
import com.babjo.deliverycommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    @Transactional
    public OrderResponseDto createOrder(Long userId, OrderRequestDto.CreateOrder request){
        List<OrderItem> orderItems = request.getOrderItems().stream()
                .map(itemDto -> OrderItem.createOrderItem(
                        itemDto.getProductId(),
                        itemDto.getProductName(),
                        itemDto.getProductPrice(),
                        itemDto.getOrderCount()
                ))
                .toList();
        Order order = Order.createOrder(
                userId,
                request.getStoreId(),
                request.getAddress(),
                request.getMessage(),
                orderItems
        );

        Order savedOrder = orderRepository.save(order);
        return convertToResponseDto(savedOrder);
    }

    private OrderResponseDto convertToResponseDto(Order order){
        List<OrderResponseDto.OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderResponseDto.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .orderPrice(item.getOrderPrice())
                        .orderCount(item.getOrderCount())
                        .build())
                .toList();
        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .userId(order.getUserId())
                .storeId(order.getStoreId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .address(order.getAddress())
                .message(order.getMessage())
                .orderItems(itemResponses)
                .createdAt(order.getCreatedAt())
                .build();
    }
}
