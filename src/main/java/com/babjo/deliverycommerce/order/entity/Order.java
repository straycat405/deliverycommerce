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

    private LocalDateTime acceptedAt;

    private Integer cookingMinutes;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> orderItems = new ArrayList<>();

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

    public void cancel(Long userId, String reason){
        if(!status.canCancel()){
            throw new IllegalStateException("취소가 불가능한 상태입니다.");
        }
        this.status = OrderStatus.CANCELED;
        this.canceledAt = LocalDateTime.now();
        this.canceledBy = userId;
        this.cancelReason = reason;
    }
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
}
