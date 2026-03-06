package com.babjo.deliverycommerce.domain.order.service;

import com.babjo.deliverycommerce.domain.order.dto.OrderRequestDto;
import com.babjo.deliverycommerce.domain.order.dto.OrderResponseDto;
import com.babjo.deliverycommerce.domain.order.entity.Order;
import com.babjo.deliverycommerce.domain.order.entity.OrderItem;
import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
import com.babjo.deliverycommerce.domain.order.repository.OrderRepository;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
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
                .map(itemDto ->{
//                      TODO : Product 작업 완료시 상품 검증 로직 활성화
//                      Product product = productRepository.findById(itemDto.getProductId())
//                                      .orElse.Throw(() -> new IllegalArgumentException("상품 없음"));
//                      if(product.getPrice() != itemDto.getOrderPrice()){
//                          throw new IllegalArgumentException("가격 불일치");
//                      }

                        return OrderItem.createOrderItem(
                        itemDto.getProductId(),
                        itemDto.getProductName(),
                        itemDto.getOrderPrice(),
                        itemDto.getOrderCount()
                        );
                })
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
        Page<Order> orderPage = orderRepository.findByUserIdAndDeletedAtIsNull(userId, pageable);

        return orderPage.map(this::convertToResponseDto);
    }


    // 주문 상세 조회
    public OrderResponseDto getOrderDetails(UUID orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));

        if(order.isDeleted()){
            throw new CustomException(ErrorCode.ORDER_ALREADY_DELETED);
        }
        return convertToResponseDto(order);
    }


    // 주문 취소
    @Transactional
    public OrderResponseDto cancelOrder(UUID orderId, Long userId, String reason){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        if(!order.getUserId().equals(userId)){
            throw new CustomException(ErrorCode.NOT_ORDER_USER);
        }
        if(order.getStatus() != OrderStatus.CREATED){
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        order.cancel(userId,reason);
        return convertToResponseDto(order);
    }

    // 주문 내역 삭제 ( 숨김 )
    @Transactional
    public OrderResponseDto softDeleteOrder(UUID orderId, Long userId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        if(!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_ORDER_USER);
        }
        if(order.isDeleted()){
            throw new CustomException(ErrorCode.ORDER_ALREADY_DELETED);
        }
        order.softDelete(userId);
        return convertToResponseDto(order);
    }

    // 주문 접수
    @Transactional
    public OrderResponseDto acceptOrder(UUID orderId, Long ownerId, OrderRequestDto.AcceptOrder request){
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
            order.accept(ownerId, request.getCookingMinutes());
            return convertToResponseDto(order);
    }

    // 주문 상태 변경
    @Transactional
    public OrderResponseDto updateOrderStatus(UUID orderId,Long ownerId, OrderStatus status){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
        switch (status){
            case PREPARING -> order.startPreparing(ownerId);
            case PICKUP_READY -> order.readyPickup(ownerId);
            case PICKED_UP -> order.completePickup(ownerId);
            default -> throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        return convertToResponseDto(order);
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
                .cancelReason(order.getCancelReason())
                .canceledAt(order.getCanceledAt())
                .canceledBy(order.getCanceledBy())
                .cookingMinutes(order.getCookingMinutes())
                .acceptedBy(order.getAcceptedBy())
                .acceptedAt(order.getAcceptedAt())
                .preparingStartedBy(order.getPreparingStartedBy())
                .pickupReadieBy(order.getPickupReadieBy())
                .pickupBy(order.getPickupBy())
                .build();
    }


}
