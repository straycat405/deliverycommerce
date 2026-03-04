package com.babjo.deliverycommerce.domain.review.dto;

import lombok.Setter;
import lombok.Getter;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class ReviewCreateResponse {
    private UUID reviewId;
    private UUID orderId;
    private UUID storeId;
    private Integer rating;
    private String content;
    private LocalDateTime createdAt;
}
