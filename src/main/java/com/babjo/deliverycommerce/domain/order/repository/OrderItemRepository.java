package com.babjo.deliverycommerce.domain.order.repository;

import com.babjo.deliverycommerce.domain.order.entity.Order;
import com.babjo.deliverycommerce.domain.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // 특정 주문에 속한 모든 상세 항목 조회
    List<OrderItem> findAllByOrder(Order order);
}
