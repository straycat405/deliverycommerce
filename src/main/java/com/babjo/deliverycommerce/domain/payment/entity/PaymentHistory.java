package com.babjo.deliverycommerce.domain.payment.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 결제 이력 엔티티 (p_payment_history)
 * - Insert-Only: 삭제/수정 없이 이벤트 발생 시마다 저장
 * - 이전 이력을 previous_history_id 로 연결 (연결 리스트 구조)
 * - BaseEntity 상속 없이 필요한 감사 필드만 직접 정의
 */
@Entity
@Table(name = "p_payment_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_history_id", updatable = false, nullable = false)
    private UUID paymentHistoryId;

    @Column(name = "payment_id", nullable = false)
    private UUID paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private PaymentStatus eventType;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "pg_payment_key", length = 200)
    private String pgPaymentKey;

    @Column(name = "reason", length = 255)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "previous_history_id")
    private UUID previousHistoryId;

    @Builder
    private PaymentHistory(UUID paymentId, PaymentStatus eventType, Integer amount,
                           String pgPaymentKey, String reason, UUID previousHistoryId) {
        this.paymentId = paymentId;
        this.eventType = eventType;
        this.amount = amount;
        this.pgPaymentKey = pgPaymentKey;
        this.reason = reason;
        this.previousHistoryId = previousHistoryId;
        this.createdAt = LocalDateTime.now();
    }

    public static PaymentHistory create(UUID paymentId, PaymentStatus eventType, Integer amount,
                                        String pgPaymentKey, String reason, UUID previousHistoryId) {
        return PaymentHistory.builder()
                .paymentId(paymentId)
                .eventType(eventType)
                .amount(amount)
                .pgPaymentKey(pgPaymentKey)
                .reason(reason)
                .previousHistoryId(previousHistoryId)
                .build();
    }
}

