package com.babjo.deliverycommerce.domain.review.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateResponse {
    private UUID reviewId;
    private Long userId;
    // [TODO] Order 도메인 연결 후 사용
    private UUID orderId;
    private UUID storeId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
