package com.babjo.deliverycommerce.order.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum OrderStatus {
    CREATED("주문 생성"), // ERD Default
    ACCEPTED("주문 접수"),
    PREPARING("조리 중"),
    PICKUP_READY("조리 완료"),
    PICKED_UP("픽업 완료"),
    CANCELED("주문 취소"),
    REJECTED("주문 거절");

     private final String description;

    // 주문 취소 가능한 상태인지 확인
    public boolean canCancel() {
        return this == CREATED;
    }

}
