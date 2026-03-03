package com.babjo.deliverycommerce.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    CREATED("주문 생성"), // ERD Default
    PREPARING("준비 중"),
    ACCEPTED("주문 접수"),
    SHIPPING("배송 중"),
    COMPLETED("배달 완료"),
    CANCELED("주문 취소"),
    REJECTED("주문 거절");

    private final String description;

    // 주문 취소 가능한 상태인지 확인
    public boolean canCancel() {
        return this == CREATED;
    }

}
