package com.babjo.deliverycommerce.domain.payment.repository;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import com.babjo.deliverycommerce.domain.payment.entity.QPayment;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Payment> searchPayments(UUID paymentId, UUID orderId, PaymentStatus paymentStatus, Long userId) {
        QPayment payment = QPayment.payment;
        BooleanBuilder builder = new BooleanBuilder();

        // deleted_at IS NULL 은 @Where 로 자동 적용됨
        if (paymentId != null) {
            builder.and(payment.paymentId.eq(paymentId));
        }
        if (orderId != null) {
            builder.and(payment.orderId.eq(orderId));
        }
        if (paymentStatus != null) {
            builder.and(payment.paymentStatus.eq(paymentStatus));
        }
        // 관리자가 아닌 경우 본인 결제만 조회
        if (userId != null) {
            builder.and(payment.userId.eq(userId));
        }

        return queryFactory
                .selectFrom(payment)
                .where(builder)
                .orderBy(payment.createdAt.desc())
                .fetch();
    }
}

