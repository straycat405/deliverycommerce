package com.babjo.deliverycommerce.order.repository;

import com.babjo.deliverycommerce.order.entity.Order;
import com.babjo.deliverycommerce.order.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findAllByOrderByCreatedAt(Long userId);

    List<Order> findAllByStatus(OrderStatus status);
}
