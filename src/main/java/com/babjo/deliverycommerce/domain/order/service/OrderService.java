package com.babjo.deliverycommerce.domain.order.service;

import com.babjo.deliverycommerce.domain.order.dto.OrderRequestDto;
import com.babjo.deliverycommerce.domain.order.dto.OrderResponseDto;
import com.babjo.deliverycommerce.domain.order.entity.Order;
import com.babjo.deliverycommerce.domain.order.entity.OrderItem;
import com.babjo.deliverycommerce.domain.order.entity.OrderStatus;
import com.babjo.deliverycommerce.domain.order.repository.OrderRepository;
import com.babjo.deliverycommerce.domain.product.entity.Product;
import com.babjo.deliverycommerce.domain.product.repository.ProductRepository;
import com.babjo.deliverycommerce.domain.store.entity.Store;
import com.babjo.deliverycommerce.domain.store.repository.StoreRepository;
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
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;

    // 주문 생성
    @Transactional
    public OrderResponseDto.OrderDetail createOrder(Long userId, OrderRequestDto.CreateOrder request){
        List<OrderItem> orderItems = request.getOrderItems().stream()
                .map(itemDto ->{

                    Product product = productRepository.findById(itemDto.getProductId())
                                      .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
                    if(!product.getPrice().equals(itemDto.getOrderPrice())){
                          throw new CustomException(ErrorCode.PRICE_MISMATCH);
                    }
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
        return OrderResponseDto.OrderDetail.from(savedOrder);
    }

    // 고객의 주문 내역 목록 조회 ( 페이징 )
    public Page<OrderResponseDto.OrderList> getUserOders(Long userId, Pageable pageable){
        return orderRepository.findByUserIdAndDeletedAtIsNull(userId, pageable)
                .map(OrderResponseDto.OrderList::from);
    }


    // 주문 상세 조회
    public OrderResponseDto.OrderDetail getOrderDetails(UUID orderId){
        Order order = findActiveOrder(orderId);

        if(order.isDeleted()){
            throw new CustomException(ErrorCode.ORDER_ALREADY_DELETED);
        }
        return OrderResponseDto.OrderDetail.from(order);
    }


    // 주문 취소
    @Transactional
    public OrderResponseDto.OrderAction cancelOrder(UUID orderId, Long userId, String reason){
        Order order = findActiveOrder(orderId);
        if(!order.getUserId().equals(userId)){
            throw new CustomException(ErrorCode.NOT_ORDER_USER);
        }
        order.cancel(userId,reason);
        return OrderResponseDto.OrderAction.from(order, order.getCanceledAt());
    }

    // 주문 내역 삭제 ( 숨김 )
    @Transactional
    public OrderResponseDto.OrderAction softDeleteOrder(UUID orderId, Long userId){
        Order order = findActiveOrder(orderId);
        if(!order.getUserId().equals(userId)) {
            throw new CustomException(ErrorCode.NOT_ORDER_USER);
        }
        order.softDelete(userId);
        return OrderResponseDto.OrderAction.from(order, order.getDeletedAt());
    }

    // 주문 접수
    @Transactional
    public OrderResponseDto.OrderAction acceptOrder(UUID orderId, Long ownerId, Integer cookingMinutes){
        Order order = findActiveOrder(orderId);
        validateStoreOwner(order.getStoreId(),ownerId);
        order.accept(ownerId, cookingMinutes);
        return OrderResponseDto.OrderAction.from(order, order.getAcceptedAt());
    }

    // 주문 거절
    public OrderResponseDto.OrderAction rejectOrder(UUID orderId, Long ownerId, String rejectReason){
        Order order = findActiveOrder(orderId);
        validateStoreOwner(order.getStoreId(), ownerId);
        order.reject(ownerId, rejectReason);
        return OrderResponseDto.OrderAction.from(order, order.getCanceledAt());
    }

    // 주문 상태 변경
    @Transactional
    public OrderResponseDto.OrderAction updateOrderStatus(UUID orderId,Long ownerId, OrderStatus status){
        Order order = findActiveOrder(orderId);
        validateStoreOwner(order.getStoreId(),ownerId);
        switch (status){
            case PREPARING -> order.startPreparing(ownerId);
            case PICKUP_READY -> order.readyPickup(ownerId);
            case PICKED_UP -> order.completePickup(ownerId);
            default -> throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        return OrderResponseDto.OrderAction.from(order, order.getUpdatedAt());
    }

    // orderId 조회 로직 공통 메소드
    private Order findActiveOrder(UUID orderId){
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new CustomException(ErrorCode.ORDER_NOT_FOUND));
    }

    // 가게 소유권 검증 로직 공통 메소드
    private void validateStoreOwner(UUID storeId, Long ownerId){
        Store store = storeRepository.findByStoreIdAndDeletedAtIsNull(storeId)
                .orElseThrow(() -> new CustomException(ErrorCode.STORE_NOT_FOUND));
        if(!store.getOwnerId().equals(ownerId)){
            throw new CustomException(ErrorCode.STORE_FORBIDDEN);
        }
    }


}
