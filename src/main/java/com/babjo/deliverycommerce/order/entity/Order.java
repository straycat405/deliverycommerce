package com.babjo.deliverycommerce.order.entity;

import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SoftDelete(columnName = "is_deleted")
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

    private Integer cookingMinutes;

    private Long preparingStartedBy;

    private Long pickupReadieBy;

    private Long pickupBy;

    private LocalDateTime acceptedAt;

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

        order.calculateTotalPrice();
        return order;

    }

    public void addOrderItem(OrderItem orderItem){
        this.orderItems.add(orderItem);
        orderItem.setOrder(this);
    }
    private void calculateTotalPrice(){
        this.totalPrice = this.orderItems.stream()
                .mapToInt(OrderItem::getItemTotal)
                .sum();
    }

    // 주문 취소
    public void cancel(Long userId, String reason){
        if(!status.canCancel()){
            throw new IllegalStateException("취소가 불가능한 상태입니다.");
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.canceledBy = userId;
        this.cancelReason = reason;
    }

    // 주문 접수
    public void accept(Long ownerId, Integer cookingMinutes){
        if(this.status != OrderStatus.CREATED){
            throw new IllegalStateException("접수할 수 없는 주문 상태입니다.");
        }
        if(cookingMinutes == null || cookingMinutes <= 0){
            throw new IllegalArgumentException("올바른 조리 시간을 입력해주세요.");
        }

        this.status = OrderStatus.ACCEPTED;
        this.acceptedAt = LocalDateTime.now();
        this.acceptedBy = ownerId;
        this.cookingMinutes = cookingMinutes;
    }

    // 조리 시작
    public void startPreparing(Long ownerId) {
        validateStatus(OrderStatus.ACCEPTED, "조리를 시작할 수 없는 상태입니다. ");
        this.status = OrderStatus.PREPARING;
        this.preparingStartedBy = ownerId;
    }

    // 조리 완료 및 픽업 대기
    public void readyPickup(Long ownerId){
        validateStatus(OrderStatus.PREPARING, "조리 시작한 주문만 픽업 대기 상태로 변경할 수 있습니다.");
        this.status = OrderStatus.PICKUP_READY;
        this.pickupReadieBy = ownerId;
    }

    // 픽업 완료
    public void completePickup(Long ownerId){
        if(this.status != OrderStatus.PICKUP_READY){
            throw new IllegalStateException("픽업 대기중인 주문만 픽업 완료 상태로 변경 가능합니다.");
        }
        this.status = OrderStatus.PICKED_UP;
    }

    // 상태 변경 중복 코드
    private void validateStatus(OrderStatus status, String message){
        if(this.status != status){
            throw new IllegalStateException(message);
        }
    }
}
