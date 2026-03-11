package com.babjo.deliverycommerce.domain.order.entity;

import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import com.babjo.deliverycommerce.global.exception.CustomException;
import com.babjo.deliverycommerce.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name="p_order")
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID orderId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private UUID storeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.CREATED;

    @Column(nullable = false)
    private Integer totalPrice = 0;

    @Column(nullable = false)
    private String address;

    @Column(columnDefinition = "TEXT")
    private String message;

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

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

    // 주문 생성
    public static Order createOrder(Long userId, UUID storeId, String address, String message, List<OrderItem> orderItems){
        Order order = new Order();
        order.userId = userId;
        order.storeId = storeId;
        order.address = address;
        order.message = message;
        order.status = OrderStatus.CREATED;

        for(OrderItem item : orderItems){
            order.addOrderItem(item);
        }

        order.totalPrice = order.orderItems.stream()
                .mapToInt(OrderItem::getItemTotal)
                .sum();
        return order;

    }

    public void addOrderItem(OrderItem orderItem){
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }


    // 주문 취소
    public void cancel(Long userId, String reason){
        if (this.status != OrderStatus.CREATED){
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.canceledBy = userId;
        this.cancelReason = reason;
    }

    // 주문 내역 삭제 ( 숨김 )
    public void softDelete(Long userId){
        if(super.isDeleted()){
            throw new CustomException(ErrorCode.ORDER_ALREADY_DELETED);
        }
        if(this.status != OrderStatus.PICKED_UP && this.status != OrderStatus.CANCELED){
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        super.delete(userId);
    }

    // 주문 접수
    public void accept(Long ownerId, Integer cookingMinutes){
        if(this.status != OrderStatus.CREATED){
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        if(cookingMinutes == null || cookingMinutes <= 0){
            throw new CustomException(ErrorCode.INVALID_COOKING_TIME);
        }

        this.status = OrderStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
        this.acceptedBy = ownerId;
        this.cookingMinutes = cookingMinutes;
    }

    // 주문 거절
    public void reject(Long ownerId, String rejectReason){
        if(this.status != OrderStatus.CREATED){
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
        this.status = OrderStatus.REJECTED;
        this.cancelReason = rejectReason;
        this.canceledAt = LocalDateTime.now();
        this.canceledBy = ownerId;

    }

    // state : 조리 시작
    public void startPreparing(Long ownerId) {
        validateStatus(OrderStatus.ACCEPTED);
        this.status = OrderStatus.PREPARING;
        this.preparingStartedBy = ownerId;
        this.preparingStartedAt = LocalDateTime.now();
    }

    // state : 조리 완료 및 픽업 대기
    public void readyPickup(Long ownerId){
        validateStatus(OrderStatus.PREPARING);
        this.status = OrderStatus.PICKUP_READY;
        this.pickupReadieBy = ownerId;
        this.pickupReadieAt = LocalDateTime.now();
    }

    // state : 픽업 완료
    public void completePickup(Long ownerId){
        validateStatus(OrderStatus.PICKUP_READY);
        this.status = OrderStatus.PICKED_UP;
        this.pickupBy = ownerId;
        this.pickupAt = LocalDateTime.now();
    }

    // state 변경 중복 코드
    private void validateStatus(OrderStatus status){
        if(this.status != status){
            throw new CustomException(ErrorCode.INVALID_ORDER_STATUS);
        }
    }

}
