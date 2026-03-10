package com.babjo.deliverycommerce.domain.payment.entity;

import com.babjo.deliverycommerce.global.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 원장 엔티티 (p_payment)
 * - 결제 생명주기: READY -> COMPLETED / FAILED / CANCELED
 * - 상태 변경 시마다 PaymentHistory 에 이력 Insert
 */
@Entity
@Table(name = "p_payment")
@Where(clause = "deleted_at IS NULL")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_id", updatable = false, nullable = false)
    private UUID paymentId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 30)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "pg_provider", length = 30)
    private PgProvider pgProvider;

    @Column(name = "pg_payment_key", length = 200)
    private String pgPaymentKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private PaymentMethod paymentMethod;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "last_history_id")
    private UUID lastHistoryId;

    // ───────────────────────────────────────────
    // 생성
    // ───────────────────────────────────────────

    @Builder
    private Payment(Long userId, UUID orderId, Integer amount) {
        this.userId = userId;
        this.orderId = orderId;
        this.amount = amount;
        this.paymentStatus = PaymentStatus.READY;
        this.paymentMethod = PaymentMethod.CARD;
        this.pgProvider = PgProvider.TOSS;
    }

    public static Payment create(Long userId, UUID orderId, Integer amount) {
        return Payment.builder()
                .userId(userId)
                .orderId(orderId)
                .amount(amount)
                .build();
    }

    // ───────────────────────────────────────────
    // 상태 전이 메서드
    // ───────────────────────────────────────────

    /** 결제 승인 (READY → COMPLETED) */
    public void confirm(String pgPaymentKey) {
        this.pgPaymentKey = pgPaymentKey;
        this.paymentStatus = PaymentStatus.COMPLETED;
        this.approvedAt = LocalDateTime.now();
    }

    /** 결제 실패 (READY → FAILED) */
    public void fail() {
        this.paymentStatus = PaymentStatus.FAILED;
    }

    /** 결제 취소 (COMPLETED or READY → CANCELED) */
    public void cancel() {
        this.paymentStatus = PaymentStatus.CANCELED;
    }

    /** 최신 이력 ID 갱신 */
    public void updateLastHistoryId(UUID historyId) {
        this.lastHistoryId = historyId;
    }
}

