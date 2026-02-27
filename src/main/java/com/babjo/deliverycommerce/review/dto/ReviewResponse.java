package com.babjo.deliverycommerce.review.dto;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.UUID;

public class ReviewResponse {
    private UUID reviewId;
    private UUID orderId;
    private UUID storeId;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
