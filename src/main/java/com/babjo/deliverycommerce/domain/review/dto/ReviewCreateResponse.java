package com.babjo.deliverycommerce.domain.review.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ReviewCreateResponse {
    private UUID reviewId;
    private UUID orderId;
    private UUID storeId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
