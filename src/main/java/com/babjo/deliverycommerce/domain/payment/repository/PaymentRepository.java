package com.babjo.deliverycommerce.domain.payment.repository;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID>, PaymentRepositoryCustom {

    // 주문 ID 기준 결제 존재 여부 확인 (이미 결제된 주문 중복 방지)
    boolean existsByOrderId(UUID orderId);

    // 주문 ID로 결제 단건 조회 (@Where 적용 → deleted_at IS NULL 자동 필터)
    Optional<Payment> findByOrderId(UUID orderId);

    // userId 기준 목록 조회
    List<Payment> findAllByUserId(Long userId);

    // ── 관리자용: 삭제된 데이터 포함 조회 ─────────────────────────────────
    @Query(value = "SELECT * FROM p_payment WHERE payment_id = :id", nativeQuery = true)
    Optional<Payment> findByIdForAdmin(@Param("id") UUID id);
}

