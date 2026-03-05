package com.babjo.deliverycommerce.domain.order.repository;

import com.babjo.deliverycommerce.domain.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {

    // 삭제되지 않은 주문 조회
    Optional<Order> findByIdAndDeletedAtIsNull(UUID orderId);

    // 사용자의 주문 내역 페이징 조회 ( 삭제한 내역 숨김 )
    Page<Order> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);


}
