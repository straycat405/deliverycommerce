package com.babjo.deliverycommerce.order.service;

import com.babjo.deliverycommerce.order.dto.OrderRequestDto;
import com.babjo.deliverycommerce.order.dto.OrderResponseDto;
import com.babjo.deliverycommerce.order.entity.Order;
import com.babjo.deliverycommerce.order.entity.OrderItem;
import com.babjo.deliverycommerce.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    // 주문 생성
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

    // 고객의 주문 내역 목록 조회 ( 페이징 )
    public Page<OrderResponseDto> getUserOders(Long userId, Pageable pageable){
        Page<Order> orderPage = orderRepository.findAllByOrderByCreatedAt(userId, pageable);

        return orderPage.map(this::convertToResponseDto);
    }


    // 주문 상세 조회
    public OrderResponseDto getOrderDetails(UUID orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("해당 주문을 찾을 수 없습니다."));
        return convertToResponseDto(order);
    }


    // 주문 취소
    @Transactional
    public void cancelOrder(UUID orderId, Long userId, String reason){

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("취소할 주문이 존재하지 않습니다."));

        if(!order.getUserId().equals(userId)){
            throw new IllegalStateException("본인의 주문만 취소 할 수 있습니다.");
        }

        order.cancel(userId,reason);

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
