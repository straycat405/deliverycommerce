package com.babjo.deliverycommerce.order.entity;

import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SoftDelete;

import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SoftDelete(columnName = "is_deleted")
public class OrderItem extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @Column(nullable = false)
    private UUID productId;

    @Column(nullable = false)
    private String productName;

    @Column(nullable = false)
    private Integer orderPrice;

    @Column(nullable = false)
    private Integer orderCount;

    public static OrderItem createOrderItem(UUID productId, String productName, Integer orderPrice, Integer orderCount){
        OrderItem orderItem = new OrderItem();
        orderItem.productId = productId;
        orderItem.productName = productName;
        orderItem.orderPrice = orderPrice;
        orderItem.orderCount = orderCount;
        return orderItem;
    }

    public void setOrder(Order order){
        this.order = order;
    }

    public int getItemTotal(){
        return this.orderPrice * this.orderCount;
    }
}
