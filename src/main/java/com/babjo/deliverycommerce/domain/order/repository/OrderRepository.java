package com.babjo.deliverycommerce.domain.order.repository;

import com.babjo.deliverycommerce.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // 사용자의 주문 내역 조회
    Page<Order> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    // 특정 상태의 주문들 조회
    List<Order> findAllByStatus(OrderStatus status);

}
