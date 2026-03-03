package com.babjo.deliverycommerce.order.repository;

import com.babjo.deliverycommerce.order.entity.Order;
import com.babjo.deliverycommerce.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findAllByOrder(Order order);
}
