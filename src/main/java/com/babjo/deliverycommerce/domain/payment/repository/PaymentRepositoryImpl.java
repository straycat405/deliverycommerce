package com.babjo.deliverycommerce.domain.payment.repository;

import com.babjo.deliverycommerce.domain.payment.entity.Payment;
import com.babjo.deliverycommerce.domain.payment.entity.PaymentStatus;
import com.babjo.deliverycommerce.domain.payment.entity.QPayment;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class PaymentRepositoryImpl implements PaymentRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Payment> searchPayments(UUID paymentId, UUID orderId, PaymentStatus paymentStatus,
                                        Long userId, Pageable pageable) {
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

        // Pageable 기반 동적 정렬 적용
        OrderSpecifier<?>[] orderSpecifiers = pageable.getSort().stream()
                .map(order -> toOrderSpecifier(payment, order))
                .toArray(OrderSpecifier[]::new);

        List<Payment> content = queryFactory
                .selectFrom(payment)
                .where(builder)
                .orderBy(orderSpecifiers.length > 0 ? orderSpecifiers
                        : new OrderSpecifier[]{payment.createdAt.desc()})
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        long total = Optional.ofNullable(
                queryFactory
                        .select(payment.count())
                        .from(payment)
                        .where(builder)
                        .fetchOne()
        ).orElse(0L);

        return new PageImpl<>(content, pageable, total);
    }

    /**
     * Pageable Sort.Order → QueryDSL OrderSpecifier 변환
     * 지원 정렬 기준: createdAt, amount
     */
    private OrderSpecifier<?> toOrderSpecifier(QPayment payment, Sort.Order order) {
        Order direction = order.isAscending() ? Order.ASC : Order.DESC;
        if ("amount".equals(order.getProperty())) {
            return new OrderSpecifier<>(direction, payment.amount);
        }
        return new OrderSpecifier<>(direction, payment.createdAt);
    }
}

